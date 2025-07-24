package com.genifast.dms.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.dto.StatusUpdateDto;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.dto.request.DepartmentCreateRequest;
import com.genifast.dms.dto.request.DepartmentUpdateRequest;
import com.genifast.dms.dto.response.DepartmentResponse;
import com.genifast.dms.entity.Department;
import com.genifast.dms.entity.Organization;
import com.genifast.dms.entity.User;
import com.genifast.dms.mapper.DepartmentMapper;
import com.genifast.dms.repository.*;
import com.genifast.dms.service.DepartmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final OrganizationRepository organizationRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public void updateDepartmentStatus(Long departmentId, StatusUpdateDto statusDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Department department = findDepartmentById(departmentId);

        // Authorize: User phải là Manager của tổ chức này
        authorizeUserIsOrgManager(currentUser, department.getOrganization().getId());

        int newStatus = statusDto.getStatus();

        // 1. Cập nhật trạng thái Department
        departmentRepository.updateStatusById(departmentId, newStatus);

        // 2. Cập nhật trạng thái tất cả Category thuộc Department
        categoryRepository.updateStatusByDepartmentId(departmentId, newStatus);

        // 3. Cập nhật trạng thái tất cả Document thuộc Department
        documentRepository.updateStatusByDepartmentId(departmentId, newStatus);

        log.info("Status of Department ID {} and its children updated to {} by {}", departmentId, newStatus,
                currentUser.getEmail());
    }

    @Override
    public DepartmentResponse createDepartment(DepartmentCreateRequest createDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Organization organization = findOrgById(createDto.getOrganizationId());

        // Authorize: Chỉ manager của tổ chức mới được tạo phòng ban
        authorizeUserIsOrgManager(currentUser, organization.getId());

        // Check name conflict
        departmentRepository.findByNameAndOrganizationId(createDto.getName(), organization.getId()).ifPresent(d -> {
            throw new ApiException(ErrorCode.DEPARTMENT_ALREADY_EXISTS,
                    ErrorMessage.DEPARTMENT_ALREADY_EXISTS.getMessage());
        });

        Department department = departmentMapper.toDepartment(createDto);
        department.setOrganization(organization);
        department.setStatus(1); // Active

        Department savedDept = departmentRepository.save(department);
        log.info("Department '{}' created for organization ID {} by {}", savedDept.getName(), organization.getId(),
                currentUser.getEmail());

        return departmentMapper.toDepartmentResponse(savedDept);
    }

    @Override
    public DepartmentResponse getDepartmentById(Long departmentId) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Department department = findDepartmentById(departmentId);

        // Authorize: User phải là thành viên của tổ chức chứa phòng ban này
        authorizeUserIsMemberOfOrg(currentUser, department.getOrganization().getId());

        return departmentMapper.toDepartmentResponse(department);
    }

    @Override
    public List<DepartmentResponse> getAllDepartmentsByOrg(Long organizationId) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));

        // Authorize: User phải là thành viên của tổ chức mà họ đang yêu cầu
        authorizeUserIsMemberOfOrg(currentUser, organizationId);

        List<Department> departments = departmentRepository.findByOrganizationIdAndStatusNot(organizationId);

        return departmentMapper.toDepartmentResponseList(departments);
    }

    @Override
    public DepartmentResponse updateDepartment(Long departmentId, DepartmentUpdateRequest updateDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Department department = findDepartmentById(departmentId);

        // Authorize: Chỉ manager của tổ chức mới được sửa phòng ban
        authorizeUserIsOrgManager(currentUser, department.getOrganization().getId());

        // Nếu tên được cập nhật, kiểm tra trùng lặp trong cùng tổ chức
        if (updateDto.getName() != null && !updateDto.getName().equals(department.getName())) {
            departmentRepository.findByNameAndOrganizationId(updateDto.getName(), department.getOrganization().getId())
                    .ifPresent(d -> {
                        throw new ApiException(ErrorCode.DEPARTMENT_ALREADY_EXISTS,
                                ErrorMessage.DEPARTMENT_ALREADY_EXISTS.getMessage());
                    });
            department.setName(updateDto.getName());
        }

        if (updateDto.getDescription() != null) {
            department.setDescription(updateDto.getDescription());
        }

        Department updatedDept = departmentRepository.save(department);
        log.info("Department ID {} updated by {}", departmentId, currentUser.getEmail());

        return departmentMapper.toDepartmentResponse(updatedDept);
    }

    // ... Helper methods ...
    private void authorizeUserIsOrgManager(User user, Long orgId) {
        if (user.getIsOrganizationManager() == null || !user.getIsOrganizationManager()) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION, ErrorMessage.NO_PERMISSION.getMessage());
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

    private Organization findOrgById(Long orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORGANIZATION_NOT_EXIST,
                        ErrorMessage.ORGANIZATION_NOT_EXIST.getMessage()));
    }

    private Department findDepartmentById(Long deptId) {
        return departmentRepository.findById(deptId)
                .orElseThrow(() -> new ApiException(ErrorCode.DEPARTMENT_NOT_FOUND,
                        ErrorMessage.DEPARTMENT_NOT_FOUND.getMessage()));
    }
}
