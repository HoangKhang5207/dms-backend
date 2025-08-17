package com.genifast.dms.dto.bpmn;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VersionInfoDTO {
    private int version;

    private String name;

    private String lastModifiedBy;

    private Instant lastModifiedDate;
}
