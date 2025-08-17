package com.genifast.dms.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepsDTO {
    private Long id;

    private int stepOrder;

    private String nextKey;

    private String taskKey;

    private String condition;

    private String candidateGroup;

    private String processUser;

    private String rejectKey;
}
