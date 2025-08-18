package com.genifast.dms.service.workflow.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.engine.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.genifast.dms.dto.request.workflow.WorkflowAssignDeptRequest;
import com.genifast.dms.dto.request.workflow.WorkflowAssignEleRequest;
import com.genifast.dms.dto.request.workflow.WorkflowDeployRequest;
import com.genifast.dms.dto.request.workflow.WorkflowEleDto;
import com.genifast.dms.dto.request.workflow.WorkflowStartRequest;
import com.genifast.dms.dto.response.workflow.DeployWorkflowResponse;
import com.genifast.dms.dto.response.workflow.ProcessTResponse;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.Department;
import com.genifast.dms.entity.workflow.BpmnUpload;
import com.genifast.dms.entity.workflow.DocumentWorkflowInstance;
import com.genifast.dms.entity.workflow.Workflow;
import com.genifast.dms.entity.workflow.WorkflowEle;
import com.genifast.dms.entity.workflow.WorkflowOrg;
import com.genifast.dms.entity.workflow.WorkflowStep;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.workflow.BpmnUploadRepository;
import com.genifast.dms.repository.workflow.DocumentWorkflowInstanceRepository;
import com.genifast.dms.repository.workflow.WorkflowEleRepository;
import com.genifast.dms.repository.workflow.WorkflowOrgRepository;
import com.genifast.dms.repository.workflow.WorkflowRepository;
import com.genifast.dms.repository.workflow.WorkflowStepRepository;
import com.genifast.dms.service.workflow.WorkflowService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    private final WorkflowRepository workflowRepository;
    private final WorkflowOrgRepository workflowOrgRepository;
    private final WorkflowEleRepository workflowEleRepository;
    private final BpmnUploadRepository bpmnUploadRepository;
    private final DocumentWorkflowInstanceRepository dwiRepository;
    private final DocumentRepository documentRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public DeployWorkflowResponse deploy(WorkflowDeployRequest req) {
        log.info("[WORKFLOW] deploy bpmnUploadId={}, name={}", req.getBpmnUploadId(), req.getName());

        BpmnUpload bpmn = bpmnUploadRepository.findById(req.getBpmnUploadId())
                .orElseThrow(() -> new IllegalArgumentException("BPMN upload not found"));

        // Optional: deploy BPMN to Flowable if classpath resource is available
        String deploymentId = "N/A";
        try {
            // If path refers to classpath resource, try to deploy
            if (bpmn.getPath() != null && bpmn.getPath().startsWith("classpath:")) {
                String classpathRes = bpmn.getPath().substring("classpath:".length());
                Deployment deployment = repositoryService.createDeployment()
                        .addClasspathResource(classpathRes)
                        .name(req.getName())
                        .key(bpmn.getProcessKey())
                        .deploy();
                deploymentId = deployment.getId();
            }
        } catch (Exception ex) {
            log.warn("[WORKFLOW] Unable to deploy BPMN to Flowable, continue DB-only. reason={}", ex.getMessage());
        }

        Workflow wf = Workflow.builder()
                .version(Optional.ofNullable(bpmn.getVersion()).orElse(1))
                .documentType(req.getDocumentType())
                .name(req.getName())
                .description(req.getDescription())
                .startedRole("DEFAULT")
                .organization(bpmn.getOrganization())
                .bpmnUpload(bpmn)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isDeleted(false)
                .build();
        wf = workflowRepository.save(wf);
        final Workflow wfRef = wf;

        // mark bpmn as deployed (soft flag)
        bpmn.setIsDeployed(true);
        bpmnUploadRepository.save(bpmn);

        // Parse BPMN model to create WorkflowSteps and derive startedRole
        try {
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(bpmn.getProcessKey())
                    .latestVersion()
                    .singleResult();
            if (pd != null) {
                BpmnModel model = repositoryService.getBpmnModel(pd.getId());
                Process process = model != null ? model.getMainProcess() : null;
                if (process != null) {
                    List<UserTask> userTasks = new ArrayList<>();
                    for (FlowElement fe : process.getFlowElements()) {
                        if (fe instanceof UserTask ut) {
                            userTasks.add(ut);
                        }
                    }
                    // Derive startedRole from first user task candidate group (if any)
                    userTasks.stream().findFirst().ifPresent(first -> {
                        if (first.getCandidateGroups() != null && !first.getCandidateGroups().isEmpty()) {
                            wfRef.setStartedRole(first.getCandidateGroups().get(0));
                            workflowRepository.save(wfRef);
                        }
                    });

                    // Attempt a simple order: traverse from startEvent following outgoing flows
                    List<String> orderedTaskIds = new ArrayList<>();
                    Map<String, FlowNode> nodeMap = new java.util.HashMap<>();
                    for (FlowElement fe : process.getFlowElements()) {
                        if (fe instanceof FlowNode fn) {
                            nodeMap.put(fn.getId(), fn);
                        }
                    }
                    StartEvent start = null;
                    for (FlowElement fe : process.getFlowElements()) {
                        if (fe instanceof StartEvent se) {
                            start = se;
                            break;
                        }
                    }
                    java.util.Set<String> visited = new java.util.HashSet<>();
                    if (start != null) {
                        FlowNode current = start;
                        int guard = 0;
                        while (current != null && guard++ < 100) {
                            visited.add(current.getId());
                            // if current is user task, record it
                            if (current instanceof UserTask ut) {
                                orderedTaskIds.add(ut.getId());
                            }
                            // move to next (pick the first outgoing flow for linear flows)
                            if (current.getOutgoingFlows() != null && !current.getOutgoingFlows().isEmpty()) {
                                String nextId = current.getOutgoingFlows().get(0).getTargetRef();
                                FlowNode next = nodeMap.get(nextId);
                                if (next == null || visited.contains(next.getId())) {
                                    break;
                                }
                                current = next;
                            } else {
                                break;
                            }
                        }
                    }

                    // Fallback: if traversal empty, just use all user tasks in any order
                    if (orderedTaskIds.isEmpty() && !userTasks.isEmpty()) {
                        orderedTaskIds = userTasks.stream().map(UserTask::getId).toList();
                    }

                    // Persist WorkflowStep
                    Instant now = Instant.now();
                    List<WorkflowStep> steps = new ArrayList<>();
                    int order = 1;
                    for (String taskId : orderedTaskIds) {
                        UserTask ut = userTasks.stream().filter(u -> u.getId().equals(taskId)).findFirst().orElse(null);
                        String candidateGroup = (ut != null && ut.getCandidateGroups() != null)
                                ? String.join(",", ut.getCandidateGroups())
                                : null;
                        String nextKey = null;
                        if (order < orderedTaskIds.size()) {
                            nextKey = orderedTaskIds.get(order); // next task id
                        }
                        WorkflowStep step = WorkflowStep.builder()
                                .workflow(wf)
                                .processKey(bpmn.getProcessKey())
                                .stepOrder(order)
                                .taskKey(taskId)
                                .nextKey(nextKey)
                                .candidateGroup(candidateGroup)
                                .createdAt(now)
                                .updatedAt(now)
                                .isDeleted(false)
                                .build();
                        steps.add(step);
                        order++;
                    }
                    if (!steps.isEmpty()) {
                        workflowStepRepository.saveAll(steps);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[WORKFLOW] Parse BPMN steps failed: {}", e.getMessage());
        }

        return new DeployWorkflowResponse(deploymentId, wf.getId());
    }

    @Override
    @Transactional
    public void assignDepartments(WorkflowAssignDeptRequest req) {
        log.info("[WORKFLOW] assign dept workflowId={}, depts={}", req.getId(), req.getDepartmentIds());
        Workflow wf = workflowRepository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        // remove old (hard delete for simplicity)
        List<WorkflowOrg> olds = workflowOrgRepository.findByWorkflow_IdAndIsDeletedFalse(req.getId());
        if (!olds.isEmpty()) {
            workflowOrgRepository.deleteAll(olds);
        }

        List<WorkflowOrg> news = new ArrayList<>();
        Instant now = Instant.now();
        for (Long deptId : req.getDepartmentIds()) {
            Department deptRef = entityManager.getReference(Department.class, deptId);
            WorkflowOrg wo = WorkflowOrg.builder()
                    .workflow(wf)
                    .department(deptRef)
                    .createdAt(now)
                    .updatedAt(now)
                    .isDeleted(false)
                    .build();
            news.add(wo);
        }
        workflowOrgRepository.saveAll(news);
    }

    @Override
    @Transactional
    public void assignElements(WorkflowAssignEleRequest req) {
        log.info("[WORKFLOW] assign ele workflowId={}", req.getId());
        Workflow wf = workflowRepository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        // remove old
        List<WorkflowEle> olds = workflowEleRepository.findByWorkflow_IdAndIsDeletedFalse(req.getId());
        if (!olds.isEmpty()) {
            workflowEleRepository.deleteAll(olds);
        }

        WorkflowEleDto dto = req.getWorkflowEleDto();
        String categoryIds = dto.getCategoryIds() == null ? null
                : dto.getCategoryIds().stream().map(String::valueOf).collect(Collectors.joining(","));
        String urgency = dto.getUrgency() == null ? null : String.join(",", dto.getUrgency());
        String security = dto.getSecurity() == null ? null : String.join(",", dto.getSecurity());

        WorkflowEle ele = WorkflowEle.builder()
                .workflow(wf)
                .categoryIds(categoryIds)
                .urgency(urgency)
                .security(security)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isDeleted(false)
                .build();
        workflowEleRepository.save(ele);
    }

    @Override
    @Transactional
    public ProcessTResponse start(WorkflowStartRequest req) {
        log.info("[WORKFLOW] start workflowId={}, documentId={}", req.getWorkflowId(), req.getDocumentId());
        Workflow wf = workflowRepository.findById(req.getWorkflowId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));
        Document doc = documentRepository.findById(req.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        String processKey = wf.getBpmnUpload().getProcessKey();
        Map<String, Object> vars = new HashMap<>();
        vars.put("documentId", req.getDocumentId());
        vars.put("condition", req.getCondition());
        vars.put("startUser", req.getStartUser());
        vars.put("workflowId", wf.getId());
        vars.put("bpmnUpload_id", wf.getBpmnUpload().getId());
        // assigneeMap: lưu người đã/đang xử lý theo taskKey
        Map<String, Object> assigneeMap = new HashMap<>();
        vars.put("assigneeMap", assigneeMap);

        // businessKey = documentId
        String businessKey = String.valueOf(req.getDocumentId());
        String procInstId = runtimeService.startProcessInstanceByKey(processKey, businessKey, vars).getId();
        log.info("[WORKFLOW][START] processKey={}, businessKey={}, procInstId={}, vars={}", processKey, businessKey, procInstId, vars);

        DocumentWorkflowInstance dwi = DocumentWorkflowInstance.builder()
                .document(doc)
                .workflow(wf)
                .processInstanceId(procInstId)
                .status("running")
                .startedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(String.valueOf(req.getStartUser()))
                .updatedBy(String.valueOf(req.getStartUser()))
                .build();
        dwiRepository.save(dwi);

        Task current = taskService.createTaskQuery()
                .processInstanceId(procInstId)
                .active()
                .orderByTaskCreateTime()
                .asc()
                .singleResult();

        String taskKeyNext = current != null ? current.getTaskDefinitionKey() : null;
        log.info("[WORKFLOW][START] taskKeyNext={}, currentTaskId={}", taskKeyNext, current != null ? current.getId() : null);

        // Gán assignee cho task đầu tiên nếu có
        if (current != null && req.getProcessUser() != null) {
            try {
                String assignee = String.valueOf(req.getProcessUser());
                taskService.setAssignee(current.getId(), assignee);
                assigneeMap.put(taskKeyNext, req.getProcessUser());
                log.info("[WORKFLOW][ASSIGN_START] taskId={}, taskKey={}, assignee={}", current.getId(), taskKeyNext, assignee);
                log.info("[WORKFLOW][VARS] assigneeMap[{}] updated to {}", taskKeyNext, req.getProcessUser());
            } catch (Exception ex) {
                log.warn("[WORKFLOW] cannot set assignee for task {}: {}", current.getId(), ex.getMessage());
            }
        }

        return ProcessTResponse.builder()
                .processKey(processKey)
                .taskKeyNext(taskKeyNext)
                .processUser(req.getProcessUser())
                .build();
    }
}
