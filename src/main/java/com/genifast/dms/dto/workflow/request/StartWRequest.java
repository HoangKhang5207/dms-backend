package com.genifast.dms.dto.workflow.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StartWRequest {
  @NotNull
  private Long workflowId;

  @NotNull
  private Long documentId;

  private String condition = "DEFAULT";

  @NotNull
  private Long startUser;

  @NotNull
  private Long processUser;
}
