package com.genifast.dms.mapper;

import com.genifast.dms.dto.request.SignUpRequestDto;
import com.genifast.dms.dto.response.UserResponse;
import com.genifast.dms.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", // Tích hợp với Spring DI
        unmappedTargetPolicy = ReportingPolicy.IGNORE // Bỏ qua các trường không được map
)
public interface UserMapper {

    /**
     * Chuyển đổi từ SignUpRequestDto sang User entity.
     * Tự động map các trường có tên giống nhau.
     * Bỏ qua trường 'password' vì chúng ta sẽ xử lý mã hóa riêng.
     */
    @Mapping(target = "password", ignore = true)
    User toUser(SignUpRequestDto signUpRequestDto);

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "organization.id", target = "organizationId")
    UserResponse toUserResponse(User user);
}