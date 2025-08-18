package com.genifast.dms.dto.request.workflow;

import java.util.List;

import lombok.Data;

@Data
public class WorkflowEleDto {
    private List<Long> categoryIds; // tạm thời lưu join bằng dấu phẩy khi persist
    private List<String> urgency;
    private List<String> security;
}
