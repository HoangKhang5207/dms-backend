package com.genifast.dms.mapper;

import com.genifast.dms.dto.response.DocumentVersionResponse;
import com.genifast.dms.entity.DocumentVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentVersionMapper {

    @Mapping(source = "document.id", target = "documentId")
    DocumentVersionResponse toResponse(DocumentVersion entity);

    List<DocumentVersionResponse> toResponseList(List<DocumentVersion> entities);
}