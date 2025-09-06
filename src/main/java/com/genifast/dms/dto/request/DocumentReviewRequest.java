package com.genifast.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentReviewRequest {
    
    @NotBlank(message = "Action is required")
    private String action; // "approve" or "reject"
    
    private String details; // for approve action
    
    private String reason; // for reject action
}