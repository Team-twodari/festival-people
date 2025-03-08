package com.wootecam.festivals.domain.queue.service;

import com.wootecam.festivals.domain.queue.dto.UpdateWaitOrder;
import com.wootecam.festivals.domain.queue.repository.RedisWaitOrderListRepository;
import com.wootecam.festivals.domain.ticket.entity.TicketInfoWithId;
import com.wootecam.festivals.domain.ticket.repository.TicketInfoRedisRepository;
import com.wootecam.festivals.global.utils.TimeProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitOrderUpdateService {

    private static final long UPDATE_THRESHOLD = 5000;
    private static final int INCREMENT_VALUE = 100;

    private final RedisWaitOrderListRepository waitOrderListRepository;
    private final TicketInfoRedisRepository ticketInfoRedisRepository;

    private final TimeProvider timeProvider;

    @Scheduled(fixedRate = 5000)
    public void updateWaitOrders() {
        // 현재 판매 중인 티켓의 대기열 진입 범위 조회
        List<TicketInfoWithId> ticketsWithSaleStarted = ticketInfoRedisRepository.getTicketsWithSaleStarted();
        List<UpdateWaitOrder> waitOrders = waitOrderListRepository.getAllIn(ticketsWithSaleStarted);
        log.info("wait order list: {}", waitOrders.toString());

        // 대기열 진입 범위 갱신
        long currentTime = timeProvider.getCurrentTimeInMilli();
        Map<Long, Integer> updateTickets = new HashMap<>();
        for (UpdateWaitOrder wo : waitOrders) {
            if (isUpdatable(wo, currentTime)) {
                Integer newWaitOrder = wo.waitOrder() + INCREMENT_VALUE;
                updateTickets.put(wo.ticketId(), newWaitOrder);
            }
        }

        if (updateTickets.isEmpty()) {
            log.warn("No valid wait orders to update in bulk");
            return;
        }
        waitOrderListRepository.updateWaitOrderListBulk(updateTickets);
        log.info("Updated Wait order.");
    }

    private boolean isUpdatable(UpdateWaitOrder waitOrder, long currentTime) {
        return currentTime - waitOrder.updatedAt() >= UPDATE_THRESHOLD;
    }
}
