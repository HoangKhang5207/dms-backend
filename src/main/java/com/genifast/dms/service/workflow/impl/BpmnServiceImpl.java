package com.genifast.dms.service.workflow.impl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.genifast.dms.dto.response.workflow.BpmnUploadResponse;
import com.genifast.dms.entity.Organization;
import com.genifast.dms.entity.workflow.BpmnUpload;
import com.genifast.dms.entity.workflow.BpmnUploadHistory;
import com.genifast.dms.repository.OrganizationRepository;
import com.genifast.dms.repository.workflow.BpmnUploadHistoryRepository;
import com.genifast.dms.repository.workflow.BpmnUploadRepository;
import com.genifast.dms.service.workflow.BpmnService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BpmnServiceImpl implements BpmnService {
    private final BpmnUploadRepository bpmnUploadRepository;
    private final BpmnUploadHistoryRepository bpmnUploadHistoryRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    @Override
    public BpmnUploadResponse saveBpmn(Long organizationId, String name, MultipartFile bpmnFile, MultipartFile svgFile,
            Long bpmnUploadId, Boolean isPublished) {
        log.info("[BPMN] save organizationId={}, name={}, updateId={}", organizationId, name, bpmnUploadId);
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        String originalBpmnName = bpmnFile != null ? Objects.requireNonNullElse(bpmnFile.getOriginalFilename(), name) : name;
        String originalSvgName = svgFile != null ? svgFile.getOriginalFilename() : null;
        String processKey = deriveProcessKey(originalBpmnName);

        Instant now = Instant.now();
        boolean publish = Boolean.TRUE.equals(isPublished);

        if (bpmnUploadId == null) {
            // Create new BPMN upload
            BpmnUpload entity = BpmnUpload.builder()
                    .name(name)
                    .version(1)
                    .processKey(processKey)
                    .path(buildStoredPath(originalBpmnName))
                    .pathSvg(originalSvgName != null ? buildStoredPath(originalSvgName) : null)
                    .isPublished(publish)
                    .isDeployed(Boolean.FALSE)
                    .organization(org)
                    .createdAt(now)
                    .updatedAt(now)
                    .isDeleted(Boolean.FALSE)
                    .build();
            entity = bpmnUploadRepository.save(entity);
            // history
            persistHistory(entity, now);
            return toResponse(entity);
        } else {
            // Update existing if not deployed
            BpmnUpload entity = bpmnUploadRepository.findByIdAndOrganization_Id(bpmnUploadId, organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("BPMN upload not found"));
            if (Boolean.TRUE.equals(entity.getIsDeployed())) {
                throw new IllegalStateException("Cannot update: BPMN already deployed");
            }
            int nextVersion = (entity.getVersion() == null ? 0 : entity.getVersion()) + 1;
            entity.setName(name);
            entity.setVersion(nextVersion);
            entity.setProcessKey(processKey);
            entity.setPath(buildStoredPath(originalBpmnName));
            entity.setPathSvg(originalSvgName != null ? buildStoredPath(originalSvgName) : null);
            entity.setIsPublished(publish);
            entity.setUpdatedAt(now);
            entity = bpmnUploadRepository.save(entity);
            // history for the new version
            persistHistory(entity, now);
            return toResponse(entity);
        }
    }

    @Override
    public List<BpmnUploadResponse> listByOrganization(Long organizationId) {
        log.info("[BPMN] list organizationId={}", organizationId);
        return bpmnUploadRepository.findByOrganization_IdAndIsDeletedFalse(organizationId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public void softDelete(Long organizationId, Long id) {
        log.info("[BPMN] softDelete organizationId={}, id={}", organizationId, id);
        BpmnUpload entity = bpmnUploadRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("BPMN upload not found"));
        if (Boolean.TRUE.equals(entity.getIsDeployed())) {
            throw new IllegalStateException("Cannot delete: BPMN already deployed");
        }
        entity.setIsDeleted(Boolean.TRUE);
        entity.setUpdatedAt(Instant.now());
        bpmnUploadRepository.save(entity);
    }

    private void persistHistory(BpmnUpload entity, Instant now) {
        BpmnUploadHistory h = BpmnUploadHistory.builder()
                .bpmnUpload(entity)
                .version(entity.getVersion())
                .name(entity.getName())
                .processKey(entity.getProcessKey())
                .path(entity.getPath())
                .pathSvg(entity.getPathSvg())
                .isPublished(entity.getIsPublished())
                .isDeployed(entity.getIsDeployed())
                .organization(entity.getOrganization())
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(Boolean.FALSE)
                .build();
        bpmnUploadHistoryRepository.save(h);
    }

    private BpmnUploadResponse toResponse(BpmnUpload e) {
        BpmnUploadResponse r = new BpmnUploadResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setVersion(e.getVersion());
        r.setProcessKey(e.getProcessKey());
        r.setPath(e.getPath());
        r.setPathSvg(e.getPathSvg());
        r.setIsPublished(e.getIsPublished());
        r.setIsDeployed(e.getIsDeployed());
        r.setOrganizationId(e.getOrganization() != null ? e.getOrganization().getId() : null);
        return r;
    }

    private String deriveProcessKey(String filename) {
        if (filename == null) return "process";
        String base = filename;
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (slash >= 0) base = filename.substring(slash + 1);
        int dot = base.indexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        // Normalize to simple key (place hyphen at end to avoid escaping in Java string)
        return base.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String buildStoredPath(String filename) {
        // Chưa có module lưu file, tạm thời lưu đường dẫn logic để frontend hiển thị.
        return "/uploads/bpmn/" + filename;
    }
}
