package com.wootecam.festivals.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 통신 중 발생하는 예외를 처리하는 핸들러.
 * <p>
 * - WebSocket 메시지 처리 중 발생하는 `WebSocketException`을 처리하여 클라이언트에게 전송한다.
 * - `@SendToUser(destinations = "/queue/errors", broadcast = false)`를 사용하여 발생한 예외 메시지를 해당 클라이언트에게만 전송한다.
 * - 발생한 예외 정보를 로그로 기록한다.
 * </p>
 *
 * @author 김현준
 */
@Controller
@Slf4j
public class WebSocketExceptionHandler {

    /**
     * WebSocket 메시지 처리 중 발생한 `WebSocketException`을 처리하는 메서드.
     * <p>
     * - `@MessageExceptionHandler(WebSocketException.class)`를 사용하여 예외를 감지한다.
     * - `@SendToUser(destinations = "/queue/errors", broadcast = false)`를 사용하여 해당 요청을 보낸 사용자에게만 예외 메시지를 전송한다.
     * - 발생한 예외 정보를 로그로 기록한다.
     * </p>
     *
     * @param exception 발생한 `WebSocketException` 객체
     * @return 클라이언트에게 전송할 `WebSocketErrorResponse` 객체 (JSON 응답)
     */
    @MessageExceptionHandler(WebSocketException.class)
    @SendToUser(destinations = "/queue/errors")
    public WebSocketErrorResponse handleWebSocketAuthError(WebSocketException exception) {
        WebSocketErrorResponse webSocketErrorResponse = WebSocketErrorResponse.of(
                exception.getErrorCode().getCode(),
                exception.getErrorDescription()
        );

        // 예외 발생 로그 기록
        log.error("{}", webSocketErrorResponse);

        return webSocketErrorResponse;
    }
}
