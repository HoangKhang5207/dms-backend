package com.genifast.dms.mapper;

import com.genifast.dms.dto.response.DelegationResponse;
import com.genifast.dms.entity.Delegation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DelegationMapper {
    @Mapping(source = "delegator.id", target = "delegatorId")
    @Mapping(source = "delegatee.id", target = "delegateeId")
    @Mapping(source = "document.id", target = "documentId")
    DelegationResponse toDelegationResponse(Delegation delegation);
}