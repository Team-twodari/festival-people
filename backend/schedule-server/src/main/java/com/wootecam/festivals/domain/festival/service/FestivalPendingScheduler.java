package com.wootecam.festivals.domain.festival.service;

import static com.wootecam.festivals.domain.festival.constant.FestivalRedisStreamConstants.FESTIVAL_STREAM_GROUP;
import static com.wootecam.festivals.domain.festival.constant.FestivalRedisStreamConstants.FESTIVAL_STREAM_KEY;

import com.wootecam.festivals.global.scheduler.PendingClaimScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FestivalPendingScheduler {

    private final PendingClaimScheduler pendingClaimScheduler;

    @Scheduled(fixedRate = 60000)
    public void processFestivalPendingMessage() {
        pendingClaimScheduler.processPendingMessage(FESTIVAL_STREAM_KEY, FESTIVAL_STREAM_GROUP);
    }
}
