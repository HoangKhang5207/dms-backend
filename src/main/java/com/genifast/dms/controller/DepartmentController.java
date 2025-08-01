package com.genifast.dms.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.genifast.dms.common.dto.StatusUpdateDto;
import com.genifast.dms.dto.request.DepartmentCreateRequest;
import com.genifast.dms.dto.request.DepartmentUpdateRequest;
import com.genifast.dms.dto.response.CategoryResponse;
import com.genifast.dms.dto.response.DepartmentResponse;
import com.genifast.dms.service.CategoryService;
import com.genifast.dms.service.DepartmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;
    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZATION_MANAGER') and hasAuthority('department:create')")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentCreateRequest createDto) {
        DepartmentResponse createdDept = departmentService.createDepartment(createDto);
        return new ResponseEntity<>(createdDept, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('department:view-details')")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @GetMapping("/organization/{orgId}")
    @PreAuthorize("@authService.isMemberOfOrg(#orgId)")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartmentsByOrg(@PathVariable Long orgId) {
        return ResponseEntity.ok(departmentService.getAllDepartmentsByOrg(orgId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateRequest updateDto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, updateDto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateDepartmentStatus(@PathVariable Long id,
            @Valid @RequestBody StatusUpdateDto statusDto) {
        departmentService.updateDepartmentStatus(id, statusDto);
        return ResponseEntity.ok("Update status successfully");
    }

    @GetMapping("/{id}/categories")
    public ResponseEntity<Page<CategoryResponse>> getCategoriesByDepartment(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(categoryService.getCategoriesByDepartment(id, pageable));
    }

    @GetMapping("/{id}/categories/search")
    public ResponseEntity<List<CategoryResponse>> searchCategoriesByName(
            @PathVariable Long id,
            @RequestParam String name) {
        return ResponseEntity.ok(categoryService.searchCategoriesByName(id, name));
    }
}
