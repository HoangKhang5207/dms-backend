package com.genifast.dms.dto.request;

import java.time.Instant;

import lombok.Data;

@Data
public class DocumentShareRequest {

    private String recipientEmail;
    private Instant expiryDate; // Thời hạn chia sẻ, null nếu không có
    private Boolean isShareToExternal; // true nếu chia sẻ ra ngoài tổ chức, false nếu
}
