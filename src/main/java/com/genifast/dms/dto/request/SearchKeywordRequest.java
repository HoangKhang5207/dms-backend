package com.genifast.dms.dto.request;

import lombok.Data;

@Data
public class SearchKeywordRequest {
    private String keyword = ""; // Mặc định là chuỗi rỗng để lấy toàn bộ lịch sử
}
