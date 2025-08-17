package com.genifast.dms.dto.workflow;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEleDTO {
  @NotNull
  private List<Long> categoryIds;

  @NotNull
  private List<String> urgency;

  @NotNull
  private List<String> security;
}
