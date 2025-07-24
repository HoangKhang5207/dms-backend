package com.genifast.dms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrganizationStatusRequest {
    @NotNull(message = "Trạng thái là bắt buộc")
    private Integer status; // 0: Pending, 1: Approved, 2: Rejected, 3: Suspended
}
