package com.wootecam.festivals.domain.wait.service;

import com.wootecam.festivals.domain.ticket.entity.TicketInfo;
import com.wootecam.festivals.domain.ticket.repository.TicketInfoRedisRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketStockCountRedisRepository;
import com.wootecam.festivals.domain.wait.dto.WaitOrderResponse;
import com.wootecam.festivals.domain.wait.exception.WaitErrorCode;
import com.wootecam.festivals.domain.wait.repository.AvailablePurchaseMemberRedisRepository;
import com.wootecam.festivals.domain.wait.repository.PassOrderRedisRepository;
import com.wootecam.festivals.domain.wait.repository.WaitingRedisRepository;
import com.wootecam.festivals.domain.wait.session.WaitSessionRegistry;
import com.wootecam.festivals.domain.wait.session.WaitSessionRegistry.SessionInfo;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.global.utils.TimeProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitOrderService {

    private final WaitingRedisRepository waitingRepository;
    private final TicketStockCountRedisRepository ticketStockCountRedisRepository;
    private final PassOrderRedisRepository passOrderRedisRepository;
    private final TicketInfoRedisRepository ticketInfoRedisRepository;
    private final TimeProvider timeProvider;
    private final WaitSessionRegistry sessionRegistry;
    private final AvailablePurchaseMemberRedisRepository availablePurchaseMemberRedisRepository;
    private final WaitSessionRegistry waitSessionRegistry;
    @Value("${wait.queue.ttl-seconds}")
    private Long availablePurchaseMemberTtlSeconds;

    @Value("${wait.queue.pass-chunk-size}")
    private Long passChunkSize;

    /**
     * 사용자가 구매 페이지로 진입할 수 있는지를 사용자 대기 순서와 현재 입장 범위로 판단합니다. 사용자 대기 순서가 현재 입장 범위에 포함되고, 재고가 남았다면 재고를 차감하고, 구매 페이지로 진입할 수
     * 있습니다.
     *
     * @param ticketId
     * @param loginMemberId
     * @param waitOrder
     * @return 사용자가 구매 페이지로 진입할 수 있는지 여부, 대기열 순서
     */
    public WaitOrderResponse getWaitOrder(Long ticketId, Long loginMemberId, Long waitOrder) {
        validTicketSaleTime(ticketId);

        Boolean isWaiting = waitingRepository.exists(ticketId, loginMemberId);

        validWaitOrderWithWaiter(waitOrder, isWaiting);

        // 대기열 참가 및 대기 순서 발급, 만약 현재 입장 순서 범위라면 대기열 통과
        Long currentPassOrder = getCurrentPassOrder(ticketId);
        if (!isWaiting && waitOrder == null) {
            return getNewWaitOrderForNewUser(ticketId, loginMemberId, currentPassOrder);
        }

        validStockRemains(ticketId);

        // 대기 순서가 현재 입장 순서 범위에 포함된다면 대기열 통과 가능
        if (canPass(waitOrder, currentPassOrder)) {
            ticketStockCountRedisRepository.checkAndDecreaseStock(ticketId);
            log.debug("대기열 통과 - 사용자: {}, 대기 순서: {}", loginMemberId, waitOrder);
            return new WaitOrderResponse(true, waitOrder - currentPassOrder, waitOrder);
        }

        // 대기 순서가 현재 입장 순서 범위의 최소값보다 작거나 같다면, 이탈 유저이므로 새로운 대기 순서 발급
        if (waitOrder <= curMinPassOrder(currentPassOrder)) {
            log.debug("이탈 유저 새 대기 순서 발급 - 사용자: {}, 대기 순서: {}", loginMemberId, waitOrder);
            return getNewWaitOrderForExitedUser(ticketId, currentPassOrder);
        }

        // 대기가 현재 입장 순서 범위에 포함되지 않는다면 대기열 통과 불가
        return new WaitOrderResponse(false, waitOrder - currentPassOrder, waitOrder);
    }

    private WaitOrderResponse getNewWaitOrderForExitedUser(Long ticketId, Long currentPassOrder) {
        Long newWaitOrder = waitingRepository.getSize(ticketId);
        Long relativeWaitOrder = newWaitOrder - currentPassOrder;
        return new WaitOrderResponse(false, relativeWaitOrder, newWaitOrder);
    }

    private WaitOrderResponse getNewWaitOrderForNewUser(Long ticketId, Long loginMemberId, Long currentPassOrder) {
        Long curWaitOrder;
        curWaitOrder = joinWaitOrder(ticketId, loginMemberId);
        validStockRemains(ticketId);
        log.debug("대기열 참가 - 사용자: {}, 대기 순서: {}", loginMemberId, curWaitOrder);
        if (canPass(curWaitOrder, currentPassOrder)) {
            return new WaitOrderResponse(true, curWaitOrder - currentPassOrder, curWaitOrder);
        } else {
            return new WaitOrderResponse(false, curWaitOrder - currentPassOrder, curWaitOrder);
        }
    }

    private Long getCurrentPassOrder(Long ticketId) {
        return passOrderRedisRepository.get(ticketId);
    }

    // 티켓 판매 시간이 아닌 경우 예외 반환
    private void validTicketSaleTime(Long ticketId) {
        TicketInfo ticketInfo = ticketInfoRedisRepository.getTicketInfo(ticketId);
        if (ticketInfo == null) {
            log.warn("티켓 정보가 없습니다. ticketId: {}", ticketId);
            throw new ApiException(WaitErrorCode.INVALID_TICKET);
        }

        if (ticketInfo.isNotOnSale(timeProvider.getCurrentTime())) {
            log.warn("티켓 판매 시각이 아닙니다. ticketId: {}", ticketId);
            throw new ApiException(WaitErrorCode.NOT_ON_SALE);
        }
    }

    private long curMinPassOrder(Long currentPassOrder) {
        return currentPassOrder - passChunkSize;
    }

    // 재고가 없는 경우 예외 반환
    private void validStockRemains(Long ticketId) {
        if (ticketStockCountRedisRepository.getTicketStockCount(ticketId) <= 0) {
            log.warn("재고가 없습니다. ticketId: {}", ticketId);
            throw new ApiException(WaitErrorCode.NO_STOCK);
        }
    }

    // 대기열의 사용자가 대기열 번호를 보내지 않은 경우 예외 반환
    private void validWaitOrderWithWaiter(Long waitOrder, Boolean isWaiting) {
        if (isWaiting && (waitOrder == null || waitOrder < 0)) {
            throw new ApiException(WaitErrorCode.INVALID_WAIT_ORDER);
        }
    }

    private boolean canPass(Long waitOrder, Long currentPassOrder) {
        return curMinPassOrder(currentPassOrder) < waitOrder && waitOrder <= currentPassOrder + passChunkSize;
    }

    private Long joinWaitOrder(Long ticketId, Long userId) {
        if (userId != null) {
            return waitingRepository.addWaiting(ticketId, userId);
        }

        return waitingRepository.getSize(ticketId);
    }

    /**
     * 대기열에서 사용자를 제거합니다. 이 메서드는 대기열에서 사용자를 제거하고, 해당 사용자의 세션 정보를 세션 레지스트리에서 삭제합니다.
     *
     * @param sessionId
     * @param ticketId
     * @param userId
     */
    public void removeWaiting(String sessionId, Long ticketId, Long userId) {
        waitingRepository.removeWaiting(ticketId, userId);
        sessionRegistry.remove(sessionId);
        log.debug("대기열에서 사용자 제거 - 세션 ID: {}, 티켓 ID: {}, 사용자 ID: {}", sessionId, ticketId, userId);
    }

    /**
     * 대기열에서 사용자를 제거합니다. 이 메서드는 대기열에서 사용자를 제거하고, 해당 사용자의 세션 정보를 세션 레지스트리에서 삭제합니다.
     *
     * @param ticketId
     * @param passOrder
     */
    public void removeWaiting(Long ticketId, Long passOrder) {
        List<String> sessionIds = waitSessionRegistry.canPassMembersSessionId(ticketId);

        if (sessionIds.isEmpty()) {
            log.debug("대기열에서 제거할 세션이 없습니다. 티켓 ID: {}, 대기 순서: {}", ticketId, passOrder);
            return;
        }

        sessionIds.forEach(sessionId -> {
            SessionInfo sessionInfo = waitSessionRegistry.get(sessionId);
            if (sessionInfo != null && sessionInfo.order().equals(passOrder)) {
                removeWaiting(sessionId, ticketId, sessionInfo.memberId());
                addAvailablePurchaseMember(ticketId, sessionInfo.memberId());
            } else {
                log.debug("대기열에서 제거할 세션 정보가 일치하지 않습니다. 세션 ID: {}, 티켓 ID: {}, 대기 순서: {}", sessionId, ticketId,
                        passOrder);
            }
        });
    }

    /**
     * 구매 가능한 유저를 추가합니다. 이 메서드는 대기열에서 통과한 사용자를 구매 가능한 유저 목록에 추가합니다.
     * @param ticketId
     * @param memberId
     */
    public void addAvailablePurchaseMember(Long ticketId, Long memberId) {
        availablePurchaseMemberRedisRepository.addAvailableMember(ticketId, memberId,
                availablePurchaseMemberTtlSeconds);
        log.info("구매 가능 유저 추가 - 티켓 ID: {}, 사용자 ID: {}", ticketId, memberId);
    }
}
