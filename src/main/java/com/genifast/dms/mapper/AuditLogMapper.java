package com.genifast.dms.mapper;

import com.genifast.dms.dto.response.AuditLogResponse;
import com.genifast.dms.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogMapper {
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "document.id", target = "documentId")
    AuditLogResponse toAuditLogResponse(AuditLog auditLog);
}