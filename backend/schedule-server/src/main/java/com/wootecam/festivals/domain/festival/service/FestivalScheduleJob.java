package com.wootecam.festivals.domain.festival.service;

import com.wootecam.festivals.domain.festival.entity.FestivalProgressStatus;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FestivalScheduleJob implements Job {

    private final FestivalStatusUpdateService festivalStatusUpdateService;

    /**
     * Festival 스케줄러가 실행할 로직에 대해 작성한 클래스입니다. 스케줄러 실행 시 축제의 상태를 변경합니다. 축제가 시작 시간이 지났다면 진행 중 상태로 변경하고, 종료 시간이 지났다면 완료 상태로
     * 변경합니다.
     *
     * @param jobExecutionContext
     * @throws JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        log.debug("스케줄러 실행 시작");
        try {
            JobDataMap dataMap = jobExecutionContext.getJobDetail().getJobDataMap();
            Long festivalId = dataMap.getLong("festivalId");
            String statusString = dataMap.getString("festivalStatus");

            FestivalProgressStatus festivalStatus = FestivalProgressStatus.from(statusString);
            festivalStatusUpdateService.updateFestivalStatus(festivalId, festivalStatus);
            log.info("축제 ID: {}의 상태가 {}로 변경되었습니다.", festivalId, festivalStatus);
        } catch (RuntimeException e) {
            log.error("스케줄러 실행 중 오류 발생", e);

            JobExecutionException jobExecutionException = new JobExecutionException(e);
            jobExecutionException.setRefireImmediately(true);

            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "축제 정보 업데이트 스케줄러 실행 중 오류 발생", e);
        }
    }
}
