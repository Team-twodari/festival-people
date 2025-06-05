package com.wootecam.festivals.domain.wait.controller;

import com.wootecam.festivals.domain.wait.dto.WaitOrderRequest;
import com.wootecam.festivals.domain.wait.dto.WaitOrderResponse;
import com.wootecam.festivals.domain.wait.service.WaitOrderService;
import com.wootecam.festivals.domain.wait.session.WaitSessionRegistry;
import com.wootecam.festivals.global.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WaitOrderWebSocketController {

    private final WaitOrderService waitOrderService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WaitSessionRegistry sessionRegistry;

    /**
     * 대기열에 참여하는 API 이 API는 WebSocket을 통해 호출되며, 대기열에 참여한 사용자의 세션 정보를 등록합니다.
     *
     * @param festivalId     대기열이 속한 축제 ID
     * @param ticketId       대기열이 속한 티켓 ID
     * @param memberId       대기열에 참여하는 사용자의 ID
     * @param request        대기열 참여 요청 정보
     * @param headerAccessor WebSocket 세션 정보를 포함하는 헤더 액세서
     * @return 대기열 참여 응답
     */
    @MessageMapping("/wait/{festivalId}/tickets/{ticketId}/join")
    public void joinQueue(@DestinationVariable Long festivalId,
                          @DestinationVariable Long ticketId,
                          @AuthUser Long memberId,
                          WaitOrderRequest request,
                          SimpMessageHeaderAccessor headerAccessor) {
        WaitOrderResponse response = waitOrderService.getWaitOrder(ticketId, memberId, request.waitOrder());
        String sessionId = headerAccessor.getSessionId();
        sessionRegistry.register(sessionId, ticketId, memberId, request.waitOrder());
        messagingTemplate.convertAndSendToUser(memberId.toString(), "/queue/wait/" + ticketId, response);
        if (response.purchasable()) {
            waitOrderService.removeWaiting(sessionId, ticketId, memberId);
            waitOrderService.addAvailablePurchaseMember(ticketId, memberId);
        }
    }
}
