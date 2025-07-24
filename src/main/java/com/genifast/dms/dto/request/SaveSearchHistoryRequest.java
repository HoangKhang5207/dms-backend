package com.genifast.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveSearchHistoryRequest {
    @NotBlank(message = "Keyword không thể rỗng")
    private String keyword;

    @NotNull(message = "Loại tìm kiếm là bắt buộc")
    private Integer type;
}
