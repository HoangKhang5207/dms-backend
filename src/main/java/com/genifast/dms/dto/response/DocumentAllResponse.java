package com.genifast.dms.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAllResponse {
    private Long id;
    private String title;
    private String content;
    private Integer status;
    private String type;
    private Integer accessType;
    private String originalFilename;
    private Instant createdAt;
    private Instant updatedAt;

    private CategoryBriefResponse category;
    private OrganizationBriefResponse organization;
}
