package com.wootecam.festivals.global.exception;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class WebSocketException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String errorDescription;

    public WebSocketException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errorDescription = errorCode.getMessage();
    }

    public WebSocketException(ErrorCode errorCode, String errorDescription) {
        super(errorDescription);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public WebSocketException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.errorDescription = errorCode.getMessage();
    }

    public WebSocketException(ErrorCode errorCode, String errorDescription, Throwable cause) {
        super(errorDescription, cause);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }
}
