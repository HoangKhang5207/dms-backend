package com.genifast.dms.controller;

import com.genifast.dms.dto.request.ProjectCreateRequest;
import com.genifast.dms.dto.request.ProjectMemberRequest;
import com.genifast.dms.dto.request.ProjectRoleRequest;
import com.genifast.dms.dto.request.ProjectUpdateRequest;
import com.genifast.dms.dto.response.ProjectResponse;
import com.genifast.dms.dto.response.ProjectRoleResponse;
import com.genifast.dms.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // ======== PROJECT MANAGEMENT ========

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectCreateRequest createDto) {
        ProjectResponse newProject = projectService.createProject(createDto);
        return new ResponseEntity<>(newProject, HttpStatus.CREATED);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest updateDto) {
        return ResponseEntity.ok(projectService.updateProject(projectId, updateDto));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    // ======== MEMBER MANAGEMENT ========

    @PostMapping("/{projectId}/members")
    public ResponseEntity<ProjectResponse> addMember(@PathVariable Long projectId,
            @Valid @RequestBody ProjectMemberRequest addDto) {
        return ResponseEntity.ok(projectService.addMemberToProject(projectId, addDto));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<ProjectResponse> removeMember(@PathVariable Long projectId, @PathVariable Long userId) {
        return ResponseEntity.ok(projectService.removeMemberFromProject(projectId, userId));
    }

    @PutMapping("/{projectId}/members/{userId}/role/{newRoleId}")
    public ResponseEntity<ProjectResponse> changeMemberRole(@PathVariable Long projectId, @PathVariable Long userId,
            @PathVariable Long newRoleId) {
        return ResponseEntity.ok(projectService.changeMemberRole(projectId, userId, newRoleId));
    }

    // ======== ROLE MANAGEMENT ========

    @PostMapping("/{projectId}/roles")
    public ResponseEntity<ProjectRoleResponse> createRole(@PathVariable Long projectId,
            @Valid @RequestBody ProjectRoleRequest roleDto) {
        ProjectRoleResponse newRole = projectService.createProjectRole(projectId, roleDto);
        return new ResponseEntity<>(newRole, HttpStatus.CREATED);
    }

    @PutMapping("/{projectId}/roles/{roleId}")
    public ResponseEntity<ProjectRoleResponse> updateRole(@PathVariable Long projectId, @PathVariable Long roleId,
            @Valid @RequestBody ProjectRoleRequest roleDto) {
        return ResponseEntity.ok(projectService.updateProjectRole(projectId, roleId, roleDto));
    }

    @DeleteMapping("/{projectId}/roles/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long projectId, @PathVariable Long roleId) {
        projectService.deleteProjectRole(projectId, roleId);
        return ResponseEntity.noContent().build();
    }
}