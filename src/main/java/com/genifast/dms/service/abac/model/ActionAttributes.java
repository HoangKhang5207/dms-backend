package com.genifast.dms.service.abac.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Thuộc tính của Action (hành động) trong ABAC
 */
@Data
@AllArgsConstructor
public class ActionAttributes {
    private String action;
    
    public boolean isReadAction() {
        return "read".equalsIgnoreCase(action) || "documents:read".equalsIgnoreCase(action);
    }
    
    public boolean isWriteAction() {
        return "write".equalsIgnoreCase(action) || "documents:update".equalsIgnoreCase(action) || 
               "documents:edit".equalsIgnoreCase(action);
    }
    
    public boolean isShareAction() {
        return action != null && action.toLowerCase().contains("share");
    }
    
    public boolean isDeleteAction() {
        return "delete".equalsIgnoreCase(action) || "documents:delete".equalsIgnoreCase(action);
    }
}
