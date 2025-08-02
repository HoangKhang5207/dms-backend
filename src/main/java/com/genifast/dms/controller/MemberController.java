package com.genifast.dms.controller;

import com.genifast.dms.dto.request.DeptManagerUpdateRequest;
import com.genifast.dms.dto.response.UserResponse;
import com.genifast.dms.service.OrganizationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberController {

    private final OrganizationService organizationService;

    @GetMapping("/organizations/{orgId}/members")
    public ResponseEntity<Page<UserResponse>> getOrganizationMembers(
            @PathVariable Long orgId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(organizationService.getOrganizationMembers(orgId, pageable));
    }

    @GetMapping("/departments/{deptId}/members")
    public ResponseEntity<Page<UserResponse>> getDepartmentMembers(
            @PathVariable Long deptId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(organizationService.getDepartmentMembers(deptId, pageable));
    }

    @PutMapping("/organizations/{orgId}/members/department-manager")
    public ResponseEntity<String> updateDepartmentManagerRole(
            @PathVariable Long orgId,
            @Valid @RequestBody DeptManagerUpdateRequest deptManagerUpdateRequest) {

        organizationService.updateDepartmentManagerRole(orgId, deptManagerUpdateRequest);
        return ResponseEntity.ok("Change Department Manager successfully");
    }
}
