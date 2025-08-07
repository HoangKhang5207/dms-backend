package com.genifast.dms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.Instant;
import java.util.Set;

@Data
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;

    @JsonProperty("start_date")
    private Instant startDate;

    @JsonProperty("end_date")
    private Instant endDate;

    private Integer status; // 1-ACTIVE, 2-COMPLETED, 3-EXPIRED

    @JsonProperty("organization_id")
    private Long organizationId;

    // Thêm các thông tin hữu ích khác
    @JsonProperty("member_count")
    private int memberCount;

    @JsonProperty("document_count")
    private int documentCount;

    // Tùy chọn: có thể trả về danh sách chi tiết nếu cần
    private Set<ProjectMemberResponse> members;
    private Set<ProjectRoleResponse> roles;
}