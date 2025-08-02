package com.genifast.dms.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.genifast.dms.common.dto.StatusUpdateDto;
import com.genifast.dms.dto.request.CategoryCreateRequest;
import com.genifast.dms.dto.request.CategoryUpdateRequest;
import com.genifast.dms.dto.response.CategoryResponse;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZATION_MANAGER') or hasRole('DEPARTMENT_MANAGER') or hasAuthority('category:create')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest createDto) {
        CategoryResponse newCategory = categoryService.createCategory(createDto);
        return new ResponseEntity<>(newCategory, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MEMBER') or hasAuthority('category:view-details')")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION_MANAGER') or hasRole('DEPARTMENT_MANAGER') or hasAuthority('category:update')")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest updateDto) {
        return ResponseEntity.ok(categoryService.updateCategory(id, updateDto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ORGANIZATION_MANAGER') or hasRole('DEPARTMENT_MANAGER') or hasAuthority('category:update-status')")
    public ResponseEntity<String> updateCategoryStatus(@PathVariable Long id,
            @Valid @RequestBody StatusUpdateDto statusDto) {
        categoryService.updateCategoryStatus(id, statusDto);
        return ResponseEntity.ok("Update status successfully");
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasRole('MEMBER') or hasAuthority('category:view-documents')")
    public ResponseEntity<Page<DocumentResponse>> getDocumentsByCategory(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(categoryService.getDocumentsByCategory(id, pageable));
    }
}
