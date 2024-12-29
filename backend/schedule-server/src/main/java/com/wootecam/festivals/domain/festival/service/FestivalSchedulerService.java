package com.wootecam.festivals.domain.festival.service;

import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.festival.entity.FestivalProgressStatus;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;

/**
 * 축제의 시작 시간과 종료 시간을 스케줄링하는 서비스입니다. 서버 재시작 시 모든 축제의 상태를 갱신하고 향후 상태 변경을 스케줄링합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalSchedulerService {

    private final Scheduler scheduler;

    /**
     * 축제의 시작 시간과 종료 시간을 스케줄링합니다. FestivalService에서 축제를 생성할 때 호출됩니다.
     *
     * @param festival 스케줄링할 축제
     */
    public void scheduleStatusUpdate(Festival festival) {
        log.debug("Festival 스케줄링 - ID: {}", festival.getId());
        log.debug("현재 시간 : {}", LocalDateTime.now());
        log.debug("시작 시간 : {}", festival.getStartTime());
        log.debug("종료 시간 : {}", festival.getEndTime());

        scheduleStartTimeUpdate(festival);
        scheduleEndTimeUpdate(festival);
    }

    /**
     * 축제의 시작 시간을 스케줄링합니다.
     *
     * @param festival
     */
    private void scheduleStartTimeUpdate(Festival festival) {
        scheduleStatusChange(festival.getId(), FestivalProgressStatus.ONGOING, festival.getStartTime(), "시작");
    }

    /**
     * 축제의 종료 시간을 스케줄링합니다.
     *
     * @param festival
     */
    private void scheduleEndTimeUpdate(Festival festival) {
        scheduleStatusChange(festival.getId(), FestivalProgressStatus.COMPLETED, festival.getEndTime(), "종료");
    }

    /**
     * 축제의 상태 변경을 스케줄링합니다.
     *
     * @param festivalId  축제 ID
     * @param status      변경할 상태
     * @param triggerTime 트리거 시간
     * @param eventType   이벤트 타입
     */
    public void scheduleStatusChange(Long festivalId, FestivalProgressStatus status, LocalDateTime triggerTime,
                                     String eventType) {
        try {
            String jobKey = "festivalJob_" + festivalId + "_" + eventType;
            String triggerKey = "festivalTrigger_" + festivalId + "_" + eventType;

            JobDetail jobDetail = JobBuilder.newJob(FestivalScheduleJob.class)
                    .withIdentity(jobKey, "festivalGroup")
                    .usingJobData("festivalId", festivalId)
                    .usingJobData("festivalStatus", status.name())
                    .storeDurably(false)
                    .build();

            int priority = eventType.equals("시작") ? 10 : 5;

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey, "festivalGroup")
                    .startAt(java.sql.Timestamp.valueOf(triggerTime))
                    .withPriority(priority)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                    .build();

            if (scheduler.checkExists(jobDetail.getKey())) {
                log.debug("이미 등록된 작업 - 축제 ID: {}, 이벤트 타입: {}", festivalId, eventType);
            } else {
                scheduler.scheduleJob(jobDetail, trigger);
                log.debug("축제 ID: {}의 {} 스케줄링 완료 (트리거 시간: {}).", festivalId, eventType, triggerTime);
            }
        } catch (SchedulerException e) {
            log.error("스케줄링 중 오류 발생. 축제 ID: {}, 이벤트 타입: {}", festivalId, eventType, e);
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }
}
