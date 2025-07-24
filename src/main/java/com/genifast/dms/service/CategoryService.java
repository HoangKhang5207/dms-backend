package com.genifast.dms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.genifast.dms.common.dto.StatusUpdateDto;
import com.genifast.dms.dto.request.CategoryCreateRequest;
import com.genifast.dms.dto.request.CategoryUpdateRequest;
import com.genifast.dms.dto.response.CategoryResponse;

public interface CategoryService {
    CategoryResponse createCategory(CategoryCreateRequest createDto);

    CategoryResponse getCategoryById(Long categoryId);

    CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest updateDto);

    void updateCategoryStatus(Long categoryId, StatusUpdateDto statusDto);

    Page<CategoryResponse> getCategoriesByDepartment(Long departmentId, Pageable pageable);

    List<CategoryResponse> searchCategoriesByName(Long departmentId, String name);
}
