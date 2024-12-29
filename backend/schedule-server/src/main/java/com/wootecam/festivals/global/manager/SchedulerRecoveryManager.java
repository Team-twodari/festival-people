package com.wootecam.festivals.global.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerRecoveryManager {

    private final Scheduler scheduler;

    /**
     * 서버 재시작 시 모든 축제의 상태를 갱신하고 향후 상태 변경을 스케줄링합니다. 이미 종료된 축제는 완료 상태로 변경하고, 진행 중인 축제는 진행 중 상태로 변경합니다. 아직 시작하지 않은 축제는 시작 및
     * 종료 시간을 스케줄링합니다.
     */
    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void scheduleAllFestivals() {
        try {
            log.info("Quartz 스케줄러 복구 시작");

            scheduler.start();

            log.info("Quartz 스케줄러 복구 완료");
        } catch (SchedulerException e) {
            log.error("Quartz 스케줄러 복구 중 오류 발생", e);
        }
    }
}
