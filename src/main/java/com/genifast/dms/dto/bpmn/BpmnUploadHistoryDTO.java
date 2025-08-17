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
public class BpmnUploadHistoryDTO {
  private Long id;

  private Long bpmnUploadId;

  private int version;

  private String processKey;

  private String name;

  private String path;

  private String pathSvg;

  // private String startedRole;

  private Boolean isDraft;

  private Boolean isDeployed;

  private Long organizationId;

  private String lastModifiedBy;

  private Instant lastModifiedDate;
}
