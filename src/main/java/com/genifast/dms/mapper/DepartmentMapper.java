package com.genifast.dms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.genifast.dms.dto.request.DepartmentCreateRequest;
import com.genifast.dms.dto.response.DepartmentResponse;
import com.genifast.dms.entity.Department;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DepartmentMapper {
    @Mapping(source = "organization.id", target = "organizationId")
    DepartmentResponse toDepartmentResponse(Department department);

    List<DepartmentResponse> toDepartmentResponseList(List<Department> departments);

    @Mapping(target = "organization", ignore = true)
    Department toDepartment(DepartmentCreateRequest createDto);
}