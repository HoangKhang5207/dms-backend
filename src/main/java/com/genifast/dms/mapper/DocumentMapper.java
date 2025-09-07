package com.genifast.dms.mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.User;
import com.genifast.dms.entity.enums.DocumentType;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName") // Bổ sung
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "project.id", target = "projectId") // Bổ sung
    @Mapping(source = "documentType", target = "documentType", qualifiedByName = "mapDocumentTypeToCode")
    @Mapping(source = "recipients", target = "recipients", qualifiedByName = "usersToEmails") // Bổ sung
    DocumentResponse toDocumentResponse(Document document);

    List<DocumentResponse> toDocumentResponseList(List<Document> documents);

    // Bổ sung: Custom mapping method để chuyển đổi Set<User> thành Set<String>
    // (emails)
    @Named("usersToEmails")
    default Set<String> usersToEmails(Set<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
                .map(User::getEmail)
                .collect(Collectors.toSet());
    }

    @Named("mapDocumentTypeToCode")
    default String mapDocumentTypeToCode(DocumentType type) {
        return type != null ? type.getCode() : null;
    }
}
