package com.wootecam.festivals.domain.queue.service;

import static com.wootecam.festivals.global.utils.TimeProvider.KTC_ZONE;

import com.wootecam.festivals.domain.queue.dto.UpdateWaitOrder;
import com.wootecam.festivals.domain.queue.repository.RedisWaitOrderListRepository;
import com.wootecam.festivals.domain.ticket.entity.TicketInfo;
import com.wootecam.festivals.domain.ticket.repository.TicketInfoRedisRepository;
import com.wootecam.festivals.global.utils.TimeProvider;
import java.util.List;
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
        List<UpdateWaitOrder> waitOrders = waitOrderListRepository.getAllWaitOrder();
        log.info("wait order list: " + waitOrders.toString());

        long currentTime = timeProvider.getCurrentTimeInMilli();
        for (UpdateWaitOrder wo : waitOrders) {
            if (isUpdatable(wo, currentTime)) {
                Integer newWaitOrder = wo.waitOrder() + INCREMENT_VALUE;
                try {
                    waitOrderListRepository.updateWaitOrderList(wo.ticketId(), newWaitOrder);
                    log.info("Updated Wait order successfully: ticket:" + wo.ticketId()
                            + ", newWaitOrder: " + newWaitOrder);
                } catch (Exception e) {
                    log.error("Cannot update wait order: ticket: " + wo.ticketId() + ", newWaitOrder: "
                            + newWaitOrder, e);
                }
            }
        }
    }

    private boolean isUpdatable(UpdateWaitOrder waitOrder, long currentTime) {
        TicketInfo ticketInfo = ticketInfoRedisRepository.getTicketInfo(waitOrder.ticketId());
        long startMilliTime = ticketInfo.startSaleTime().toInstant(KTC_ZONE).toEpochMilli();
        long endMilliTime = ticketInfo.endSaleTime().toInstant(KTC_ZONE).toEpochMilli();

        return startMilliTime <= currentTime && currentTime <= endMilliTime
                && currentTime - waitOrder.updatedAt() >= UPDATE_THRESHOLD;
    }
}
