package com.genifast.dms.common.constant;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi dịch vụ (ServiceCode) và HTTP status.
 */
public enum ErrorCode {

    // 400XXX – Bad Request
    INVALID_REQUEST(400001, HttpStatus.BAD_REQUEST),

    // authentication
    USER_NOT_FOUND(400002, HttpStatus.BAD_REQUEST),
    INVALID_NAME(400003, HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(400004, HttpStatus.BAD_REQUEST),
    INVALID_PHONE(400005, HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(400006, HttpStatus.BAD_REQUEST),
    USERNAME_ALREADY_EXISTS(400007, HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(400009, HttpStatus.BAD_REQUEST),
    INVALID_USERNAME(400010, HttpStatus.BAD_REQUEST),
    INVALID_USER(400011, HttpStatus.BAD_REQUEST),
    USER_SOCIAL_NOT_EXIST(400012, HttpStatus.BAD_REQUEST),
    REFRESH_TOKEN_NOT_FOUND(400013, HttpStatus.BAD_REQUEST),

    // organization
    ORGANIZATION_ALREADY_EXISTS(400014, HttpStatus.BAD_REQUEST),
    INVALID_ORGANIZATION_NAME(400015, HttpStatus.BAD_REQUEST),
    ORGANIZATION_NOT_EXIST(400016, HttpStatus.BAD_REQUEST),
    CANNOT_ACCESS_ORGANIZATION(400017, HttpStatus.BAD_REQUEST),
    USER_IN_ANOTHER_ORGANIZATION(400018, HttpStatus.BAD_REQUEST),
    USER_REQUEST_CREATE_ORGANIZATION(400019, HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_ORGANIZATION(400020, HttpStatus.BAD_REQUEST),

    // department
    DEPARTMENT_NOT_FOUND(400021, HttpStatus.BAD_REQUEST),
    DEPARTMENT_ALREADY_EXISTS(400022, HttpStatus.BAD_REQUEST),
    USER_NOT_IN_DEPARTMENT(400023, HttpStatus.BAD_REQUEST),

    // category
    CATEGORY_NOT_FOUND(400023, HttpStatus.BAD_REQUEST),
    CATEGORY_ALREADY_EXISTS(400024, HttpStatus.BAD_REQUEST),
    INVALID_CATEGORY_NAME(400025, HttpStatus.BAD_REQUEST),
    INVALID_STATUS(400026, HttpStatus.BAD_REQUEST),
    USER_NO_PERMISSION(400027, HttpStatus.BAD_REQUEST),

    // 403XXX - Forbidden
    ACCESS_DENIED(403001, HttpStatus.FORBIDDEN),

    // document
    DOCUMENT_NOT_FOUND(400100, HttpStatus.BAD_REQUEST),

    // user
    USER_NOT_IN_ORGANIZATION(400200, HttpStatus.BAD_REQUEST),
    USER_INACTIVE(400201, HttpStatus.BAD_REQUEST),
    USER_EMAIL_NOT_VERIFIED(400202, HttpStatus.BAD_REQUEST),

    // product
    PRODUCT_NOT_EXIST(400300, HttpStatus.BAD_REQUEST),

    // 401XXX
    INVALID_PASSWORD(401001, HttpStatus.BAD_REQUEST),

    // 500XXX
    INTERNAL_ERROR(500001, HttpStatus.INTERNAL_SERVER_ERROR)

    ;

    private final int serviceCode;
    private final HttpStatus httpStatus;

    ErrorCode(int serviceCode, HttpStatus httpStatus) {
        this.serviceCode = serviceCode;
        this.httpStatus = httpStatus;
    }

    public int getServiceCode() {
        return serviceCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
