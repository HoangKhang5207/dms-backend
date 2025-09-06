package com.genifast.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentSignRequest {
    
    @NotBlank(message = "Action is required")
    private String action; // "sign" or "stamp"
    
    private String details; // signature/stamp details
}