package com.genifast.dms.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class InviteUsersRequest {
    @NotEmpty
    private List<@Valid UserInvite> users;

    @Data
    public static class UserInvite {
        @NotBlank(message = "Email không được để trống")
        @Pattern(regexp = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$", message = "Định dạng email không hợp lệ")
        private String email;

        @JsonProperty("department_id")
        @NotNull(message = "Id của phòng ban là bắt buộc")
        private Long departmentId;
    }
}
