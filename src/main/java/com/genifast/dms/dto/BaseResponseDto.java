package com.genifast.dms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponseDto implements Serializable {

    @JsonInclude(Include.NON_NULL)
    private String message;

    @JsonInclude(Include.NON_NULL)
    @JsonProperty(value = "error_code")
    private String errorCode;

    private boolean success;

    @JsonInclude(Include.NON_NULL)
    private Object data;

    public static BaseResponseDto error(String message) {
        return BaseResponseDto.builder()
                .success(false)
                .message(message).build();
    }

    public static BaseResponseDto error(String message, String errorCode) {
        return BaseResponseDto.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message).build();
    }

    public static BaseResponseDto error(String message, Object data) {
        return BaseResponseDto.builder()
                .success(false)
                .data(data)
                .message(message).build();
    }

    public static BaseResponseDto error(String message, String errorCode, Object data) {
        return BaseResponseDto.builder()
                .success(false)
                .data(data)
                .errorCode(errorCode)
                .message(message).build();
    }

    public static BaseResponseDto success(String message) {
        return BaseResponseDto.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static BaseResponseDto success(Object data) {
        return BaseResponseDto.builder()
                .success(true)
                .data(data)
                .build();
    }

    public static BaseResponseDto success(Object data, String message) {
        return BaseResponseDto.builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }
}
