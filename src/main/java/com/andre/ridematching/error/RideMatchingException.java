package com.andre.ridematching.error;

import java.util.Objects;

public class RideMatchingException extends RuntimeException {

    private final ErrorCode errorCode;

    public RideMatchingException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "Error code must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
