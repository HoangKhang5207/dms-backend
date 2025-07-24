package com.genifast.dms.mapper;

import com.genifast.dms.dto.response.OrganizationResponse;
import com.genifast.dms.entity.Organization;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationMapper {

    OrganizationResponse toOrganizationResponse(Organization organization);

    // Xử lý logic tùy chỉnh sau khi các trường cơ bản đã được map
    @AfterMapping
    default void calculatePercentDataUsed(@MappingTarget OrganizationResponse dto, Organization organization) {
        if (organization.getLimitData() != null && organization.getLimitData() > 0) {
            long percent = (organization.getDataUsed() * 100) / organization.getLimitData();
            dto.setPercentDataUsed(percent);
        } else {
            dto.setPercentDataUsed(0L);
        }
    }
}
