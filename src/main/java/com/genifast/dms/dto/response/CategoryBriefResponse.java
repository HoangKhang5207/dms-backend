package com.genifast.dms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBriefResponse {
    private Long id;
    private String name;
    private String description;
}
