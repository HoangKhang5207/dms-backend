package com.genifast.dms.dto.task.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTResponse {
    private String processKey;

    private String taskKey;

    private Long processUser;
}
