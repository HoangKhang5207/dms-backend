package com.genifast.dms.service;

import java.util.List;

import com.genifast.dms.common.dto.StatusUpdateDto;
import com.genifast.dms.dto.request.DepartmentCreateRequest;
import com.genifast.dms.dto.request.DepartmentUpdateRequest;
import com.genifast.dms.dto.response.DepartmentResponse;

public interface DepartmentService {
    DepartmentResponse createDepartment(DepartmentCreateRequest createDto);

    DepartmentResponse getDepartmentById(Long departmentId);

    List<DepartmentResponse> getAllDepartmentsByOrg(Long organizationId);

    DepartmentResponse updateDepartment(Long departmentId, DepartmentUpdateRequest updateDto);

    void updateDepartmentStatus(Long departmentId, StatusUpdateDto statusDto);
}
