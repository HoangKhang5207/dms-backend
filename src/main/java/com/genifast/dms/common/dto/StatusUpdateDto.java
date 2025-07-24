package com.genifast.dms.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateDto {
    @NotNull(message = "Trạng thái là bắt buộc")
    private Integer status;
}
