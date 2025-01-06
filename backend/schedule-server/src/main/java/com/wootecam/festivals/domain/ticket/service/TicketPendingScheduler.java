package com.wootecam.festivals.domain.ticket.service;

import static com.wootecam.festivals.domain.ticket.constant.TicketRedisStreamConstants.TICKET_STREAM_GROUP;
import static com.wootecam.festivals.domain.ticket.constant.TicketRedisStreamConstants.TICKET_STREAM_KEY;

import com.wootecam.festivals.global.scheduler.PendingClaimScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TicketPendingScheduler {

    private final PendingClaimScheduler pendingClaimScheduler;

    @Scheduled(fixedRate = 60000)
    public void processTicketPendingMessage() {
        pendingClaimScheduler.processPendingMessage(TICKET_STREAM_KEY, TICKET_STREAM_GROUP);
    }
}
