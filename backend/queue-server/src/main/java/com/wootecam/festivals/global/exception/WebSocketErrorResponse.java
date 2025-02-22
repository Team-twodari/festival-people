package com.wootecam.festivals.global.exception;

public class WebSocketErrorResponse {
    private final String errorCode;
    private final String message;

    public WebSocketErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public static WebSocketErrorResponse of(String errorCode, String message) {
        return new WebSocketErrorResponse(errorCode, message);
    }
}
