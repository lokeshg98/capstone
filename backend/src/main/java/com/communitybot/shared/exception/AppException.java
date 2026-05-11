package com.communitybot.shared.exception;

import lombok.Getter;

/**
 * Thrown for all known, expected application errors.
 * Controllers never catch this — {@link GlobalExceptionHandler} maps it to JSON.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
