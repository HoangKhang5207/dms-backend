package com.genifast.dms.dto.request.workflow;

import lombok.Data;

@Data
public class TaskProcessRequest {
    private Long processUser; // người xử lý bước tiếp theo
    private String condition; // APPROVE | REJECT | DEFAULT
}
