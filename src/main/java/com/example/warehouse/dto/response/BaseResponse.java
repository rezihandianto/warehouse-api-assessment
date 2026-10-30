package com.example.warehouse.dto.response;

public record BaseResponse<T>(ResultStatus responseStatus, String responseMessage, T responseData) {

    public static <T> BaseResponse<T> success(String message, T responseData) {
        return new BaseResponse<>(ResultStatus.SUCCESS, message, responseData);
    }

    public static <T> BaseResponse<T> failed(String message) {
        return new BaseResponse<>(ResultStatus.FAILED, message, null);
    }

    public static <T> BaseResponse<T> failed(String message, T responseData) {
        return new BaseResponse<>(ResultStatus.FAILED, message, responseData);
    }
}
