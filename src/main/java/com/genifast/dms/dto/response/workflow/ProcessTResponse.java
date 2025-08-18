package com.genifast.dms.dto.response.workflow;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProcessTResponse {
    private String processKey;
    private String taskKeyNext;
    private Long processUser;
    private Long processUserNext;
}
