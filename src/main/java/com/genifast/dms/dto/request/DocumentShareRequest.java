package com.genifast.dms.dto.request;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentShareRequest {

    // New: support lookup by user ID directly (preferred in Scenario 1)
    @JsonProperty("recipient_id")
    @JsonAlias({"recipientId"})
    private Long recipientId;

    // New: list of permission strings, e.g. ["documents:share:readonly"]
    private List<String> permissions;

    // Legacy field kept for backward compatibility (maps to toDate if provided)
    @Schema(hidden = true)
    private Instant expiryDate; // Thời hạn chia sẻ, null nếu không có

    // New: optional time window
    @JsonProperty("from_date")
    @JsonAlias({"fromDate"})
    private Instant fromDate; // thời điểm bắt đầu có hiệu lực
    @JsonProperty("to_date")
    @JsonAlias({"toDate"})
    private Instant toDate;   // thời điểm hết hạn

    @JsonProperty("is_share_to_external")
    @JsonAlias({"isShareToExternal"})
    private Boolean isShareToExternal; // true nếu chia sẻ ra ngoài tổ chức, false nếu
}
