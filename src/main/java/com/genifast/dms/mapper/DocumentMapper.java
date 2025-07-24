package com.genifast.dms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.entity.Document;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "organization.id", target = "organizationId")
    DocumentResponse toDocumentResponse(Document document);

    List<DocumentResponse> toDocumentResponseList(List<Document> documents);
}
