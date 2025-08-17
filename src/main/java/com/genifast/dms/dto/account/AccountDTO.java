package com.genifast.dms.dto.account;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO {
  @NotNull
  private Long accountId;

  @NotNull
  private String role;

  @NotNull
  private Long departmentId;

  @NotNull
  private String departmentName;

  @NotNull
  private Long organizationId;

  @NotNull
  private String organizationName;
}
