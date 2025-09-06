package com.genifast.dms.common.constant;

import java.util.Optional;

public enum ErrorMessage {
    // General
    INVALID_REQUEST("Yêu cầu không hợp lệ"),

    // authentication
    INVALID_NAME("Tên không hợp lệ"),
    INVALID_EMAIL("Email không hợp lệ"),
    INVALID_PASSWORD("Mật khẩu không hợp lệ"),
    EMAIL_ALREADY_EXISTS("Email has already existed"),
    USERNAME_ALREADY_EXISTS("Username has already existed"),
    INVALID_USERNAME("Invalid Username"),
    INVALID_USER("User không tồn tại"),
    USER_SOCIAL_NOT_EXIST("Tài khoản user social không tồn tại"),
    REFRESH_TOKEN_NOT_FOUND("Không tìm thấy refresh token của user"),

    // organization
    ORGANIZATION_ALREADY_EXISTS("Tổ chức đã tồn tại"),
    INVALID_ORGANIZATION_NAME("Tên tổ chức không hợp lệ"),
    ORGANIZATION_NOT_EXIST("Tổ chức không tồn tại"),
    NO_PERMISSION("Bạn không có quyền truy cập hay thực hiện hành động này"),
    CANNOT_ACCESS_ORGANIZATION("Không có quyền truy cập vào tổ chức"),
    USER_IN_ANOTHER_ORGANIZATION("User đã là thành viên của tổ chức khác"),
    USER_CREATE_ORGANIZATION_REQUESTED("User đã gửi yêu cầu tạo tổ chức"),
    CANNOT_DELETE_ORGANIZATION("Không xóa được tổ chức"),

    // department
    DEPARTMENT_NOT_FOUND("Phòng ban không tồn tại"),
    DEPARTMENT_ALREADY_EXISTS("Phòng ban đã tồn tại"),
    USER_NOT_IN_DEPARTMENT("Người dùng không thuộc phòng ban nào"),

    // category
    CATEGORY_NOT_FOUND("Thư mục không tồn tại"),
    CATEGORY_ALREADY_EXISTS("Thư mục đã tồn tại"),
    INVALID_CATEGORY_NAME("Tên thư mục không được trống"),
    INVALID_STATUS("Trạng thái không hợp lệ"),

    // document - ABAC và RBAC error messages
    DOCUMENT_NOT_FOUND("Không tìm thấy tài liệu"),
    CANNOT_SHARE_INTERNAL_EXTERNALLY("Không thể chia sẻ tài liệu nội bộ với người ngoài tổ chức"),
    RECIPIENT_NOT_AUTHORIZED_PRIVATE("Người nhận không có quyền truy cập tài liệu riêng tư này"),
    PRIVATE_DOCUMENT_REQUIRES_TIMEBOUND("Tài liệu riêng tư yêu cầu thiết lập thời hạn chia sẻ"),
    DOCUMENT_NOT_APPROVED_FOR_SHARING("Tài liệu chưa được phê duyệt để chia sẻ"),
    RECIPIENT_NOT_FOUND("Không tìm thấy người nhận"),
    INVALID_DATE_RANGE("Ngày bắt đầu phải trước ngày kết thúc"),
    EXTERNAL_DOCUMENT_INTERNAL_SHARE_DENIED("Tài liệu ngoại bộ không phù hợp cho chia sẻ nội bộ"),
    RECIPIENT_NOT_ACTIVE("Người nhận không hoạt động"),

    // user
    USER_NOT_IN_ORGANIZATION("Người dùng không thuộc tổ chức"),
    USER_INACTIVE("Người dùng không hoạt động"),
    USER_EMAIL_NOT_VERIFIED("Tài khoản chưa được xác minh"),

    // product
    PRODUCT_NOT_EXIST("Sản phẩm không tồn tại");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    /**
     * Lấy thông báo lỗi.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Nếu cần tìm enum từ key (ví dụ: "INVALID_EMAIL"):
     */
    public static Optional<ErrorMessage> fromKey(String key) {
        try {
            return Optional.of(ErrorMessage.valueOf(key));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
