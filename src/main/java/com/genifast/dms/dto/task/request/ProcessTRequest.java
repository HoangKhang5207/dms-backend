package com.genifast.dms.dto.task.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTRequest {
    private Long processUser;

    private String condition = "DEFAULT";
}
