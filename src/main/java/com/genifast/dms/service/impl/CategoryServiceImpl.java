package com.genifast.dms.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.dto.StatusUpdateDto;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.dto.request.CategoryCreateRequest;
import com.genifast.dms.dto.request.CategoryUpdateRequest;
import com.genifast.dms.dto.response.CategoryResponse;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.entity.Category;
import com.genifast.dms.entity.Department;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.User;
import com.genifast.dms.mapper.CategoryMapper;
import com.genifast.dms.mapper.DocumentMapper;
import com.genifast.dms.repository.CategoryRepository;
import com.genifast.dms.repository.DepartmentRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.CategoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper; // MapStruct Mapper
    private final DocumentMapper documentMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest createDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Department department = findDepartmentById(createDto.getDepartmentId());

        // 1. Authorize: User phải là Manager của phòng ban hoặc của tổ chức
        authorizeUserIsDeptManagerOrOrgManager(currentUser, department);

        // 2. Kiểm tra tên category trùng lặp trong cùng phòng ban
        categoryRepository.findByNameAndDepartmentId(createDto.getName(), department.getId()).ifPresent(c -> {
            throw new ApiException(ErrorCode.CATEGORY_ALREADY_EXISTS,
                    ErrorMessage.CATEGORY_ALREADY_EXISTS.getMessage());
        });

        // 3. Tạo Category
        Category category = categoryMapper.toCategory(createDto);
        category.setDepartment(department);
        category.setOrganization(department.getOrganization());
        category.setStatus(1); // Active

        if (createDto.getParentCategoryId() != null) {
            Category parent = findCategoryById(createDto.getParentCategoryId());
            category.setParentCategory(parent);
        }

        Category savedCategory = categoryRepository.save(category);
        log.info("Category '{}' created in department ID {} by {}", savedCategory.getName(), department.getId(),
                currentUser.getEmail());

        return categoryMapper.toCategoryResponse(savedCategory);
    }

    @Override
    @Transactional
    public void updateCategoryStatus(Long categoryId, StatusUpdateDto statusDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Category category = findCategoryById(categoryId);

        // Authorize: User phải là Manager
        authorizeUserIsDeptManagerOrOrgManager(currentUser, category.getDepartment());

        // Cập nhật trạng thái Category
        categoryRepository.updateStatusById(categoryId, statusDto.getStatus());

        // Cập nhật trạng thái của tất cả Document thuộc Category này
        documentRepository.updateStatusByCategoryId(categoryId, statusDto.getStatus());
        log.info("Status of Category ID {} and its documents updated to {} by {}", categoryId, statusDto.getStatus(),
                currentUser.getEmail());
    }

    // ... Implement các phương thức còn lại (get, update, search) với logic tương
    // tự ...
    @Override
    public CategoryResponse getCategoryById(Long categoryId) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Category category = findCategoryById(categoryId);

        // Authorize: User phải là thành viên của tổ chức chứa category này.
        authorizeUserIsMemberOfOrg(currentUser, category.getOrganization().getId());

        return categoryMapper.toCategoryResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest updateDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Category category = findCategoryById(categoryId);

        // Authorize: User phải là Manager của phòng ban hoặc của tổ chức
        authorizeUserIsDeptManagerOrOrgManager(currentUser, category.getDepartment());

        // Nếu tên được cập nhật, kiểm tra trùng lặp
        if (updateDto.getName() != null && !updateDto.getName().equals(category.getName())) {
            categoryRepository.findByNameAndDepartmentId(updateDto.getName(), category.getDepartment().getId())
                    .ifPresent(c -> {
                        throw new ApiException(ErrorCode.CATEGORY_ALREADY_EXISTS,
                                ErrorMessage.CATEGORY_ALREADY_EXISTS.getMessage());
                    });
            category.setName(updateDto.getName());
        }

        if (updateDto.getDescription() != null) {
            category.setDescription(updateDto.getDescription());
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category ID {} updated by {}", categoryId, currentUser.getEmail());

        return categoryMapper.toCategoryResponse(updatedCategory);
    }

    @Override
    public Page<CategoryResponse> getCategoriesByDepartment(Long departmentId, Pageable pageable) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Department department = findDepartmentById(departmentId);

        // Authorize: User phải là thành viên của tổ chức chứa phòng ban này.
        authorizeUserIsMemberOfOrg(currentUser, department.getOrganization().getId());

        // Status = 1 Active
        Page<Category> categoryPage = categoryRepository.findByDepartmentIdAndStatus(departmentId, 1, pageable);

        return categoryPage.map(categoryMapper::toCategoryResponse);
    }

    @Override
    public List<CategoryResponse> searchCategoriesByName(Long departmentId, String name) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Department department = findDepartmentById(departmentId);

        // Authorize: User phải là thành viên của tổ chức
        authorizeUserIsMemberOfOrg(currentUser, department.getOrganization().getId());

        List<Category> categories = categoryRepository.findByNameContainingIgnoreCaseAndDepartmentId(name,
                departmentId);

        return categories.stream().map(categoryMapper::toCategoryResponse).collect(Collectors.toList());
    }

    @Override
    public Page<DocumentResponse> getDocumentsByCategory(Long categoryId, Pageable pageable) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Category category = findCategoryById(categoryId);

        // Authorize: Người dùng phải là thành viên của tổ chức chứa thư mục này
        authorizeUserIsMemberOfOrg(currentUser, category.getOrganization().getId());

        // Gọi repository để lấy tài liệu
        Page<Document> documentPage = documentRepository.findByCategoryIdAndStatus(categoryId, 1, pageable);

        // Map kết quả sang DTO
        return documentPage.map(documentMapper::toDocumentResponse);
    }

    // --- Helper Methods ---
    private void authorizeUserIsDeptManagerOrOrgManager(User user, Department department) {
        // User phải thuộc tổ chức của phòng ban này
        if (user.getOrganization() == null
                || !user.getOrganization().getId().equals(department.getOrganization().getId())) {
            throw new ApiException(ErrorCode.USER_NOT_IN_ORGANIZATION,
                    ErrorMessage.USER_NOT_IN_ORGANIZATION.getMessage());
        }
        // User phải là Manager của tổ chức HOẶC Manager của chính phòng ban đó
        if ((user.getIsOrganizationManager() == null || !user.getIsOrganizationManager()) &&
                (user.getIsDeptManager() == null || !user.getIsDeptManager())) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION, "User must be a department or organization manager.");
        }
        // Nếu là manager phòng ban, phải đúng phòng ban mình quản lý
        if ((user.getIsDeptManager() != null && user.getIsDeptManager())
                && !user.getDepartment().getId().equals(department.getId())) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION, "User is not a manager of this specific department.");
        }
    }

    private void authorizeUserIsMemberOfOrg(User user, Long orgId) {
        if (user.getOrganization() == null || !user.getOrganization().getId().equals(orgId)) {
            throw new ApiException(ErrorCode.USER_NOT_IN_ORGANIZATION,
                    ErrorMessage.USER_NOT_IN_ORGANIZATION.getMessage());
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, ErrorMessage.INVALID_USER.getMessage()));
    }

    private Department findDepartmentById(Long deptId) {
        return departmentRepository.findById(deptId)
                .orElseThrow(() -> new ApiException(ErrorCode.DEPARTMENT_NOT_FOUND,
                        ErrorMessage.DEPARTMENT_NOT_FOUND.getMessage()));
    }

    private Category findCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND,
                        ErrorMessage.CATEGORY_NOT_FOUND.getMessage()));
    }
}
