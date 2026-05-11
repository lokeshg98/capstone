package com.communitybot.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform envelope for every REST response.
 *
 * <pre>
 * Success: { "ok": true,  "data": { ... } }
 * Error:   { "ok": false, "error": "...", "details": { ... } }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean ok,
        T data,
        String error,
        Object details
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    public static <D> ApiResponse<D> error(String message, D details) {
        return new ApiResponse<>(false, null, message, details);
    }
}
