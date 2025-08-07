package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.Instant;

@Data
public class ProjectUpdateRequest {
    @NotBlank(message = "Tên dự án không được để trống")
    @Size(min = 3, max = 255, message = "Tên dự án phải có từ 3 đến 255 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @JsonProperty("start_date")
    private Instant startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @JsonProperty("end_date")
    private Instant endDate;

    @NotNull(message = "Trạng thái không được để trống")
    // Giả sử: 1-ACTIVE, 2-COMPLETED
    private Integer status;
}