package com.genifast.dms.dto.request;

import java.util.Set;
import lombok.Data;

@Data
public class UserRoleUpdateRequestDto {
    private Set<Long> roleIds;
}