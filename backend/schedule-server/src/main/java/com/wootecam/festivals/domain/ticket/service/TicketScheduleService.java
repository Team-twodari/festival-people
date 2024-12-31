package com.wootecam.festivals.domain.ticket.service;

import static com.wootecam.festivals.domain.ticket.exception.TicketErrorCode.TICKET_NOT_FOUND;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.festival.dto.TicketResponse;
import com.wootecam.festivals.domain.ticket.entity.Ticket;
import com.wootecam.festivals.domain.ticket.repository.TicketRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketScheduleService {

    private final Scheduler scheduler;
    private final TicketRepository ticketRepository;
    private final ObjectMapper objectMapper;

    /**
     * 판매 진행중이거나 앞으로 판매될 티켓의 메타 정보와 재고를 Redis에 저장 - Ticket 의 startSaleTime, endSaleTime, remainStock
     */
    public void scheduleRedisTicketInfoUpdate(Ticket ticket) {
        TicketResponse ticketResponse = ticketRepository.findUpcomingAndOngoingSaleTickets(
                ticket.getId()).orElseThrow(() -> new ApiException(TICKET_NOT_FOUND));

        log.info("티켓 정보 업데이트 테스크 스케줄링 시작 - 티켓 ID: {}, 판매 시작 시각: {}, 판매 종료 시각: {}, 남은 재고: {}",
                ticketResponse.id(), ticketResponse.startSaleTime(), ticketResponse.endSaleTime(),
                ticketResponse.remainStock());

        try {
            String ticketToJson = objectMapper.writeValueAsString(ticketResponse);
            String jobKey = "ticketJob_" + ticketResponse.id();
            String triggerKey = "ticketJobTrigger_" + ticketResponse.id();

            JobDetail jobDetail = JobBuilder.newJob(TicketScheduleJob.class)
                    .withIdentity(jobKey, "ticketGroup")
                    .usingJobData("ticket", ticketToJson)
                    .storeDurably(false)
                    .build();

            LocalDateTime scheduleTime = ticketResponse.startSaleTime().minusMinutes(10);

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey, "ticketGroup")
                    .startAt(java.sql.Timestamp.valueOf(scheduleTime))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionIgnoreMisfires())
                    .build();

            if (scheduler.checkExists(jobDetail.getKey())) {
                log.info("이미 스케줄링된 티켓 정보 업데이트 테스크가 존재합니다. 티켓 ID: {}", ticketResponse.id());
            } else {
                scheduler.scheduleJob(jobDetail, trigger);
                log.info(
                        "Redis에 티켓 정보 업데이트 테스크 스케줄링 완료 - 티켓 ID: {}, 판매 시작 시각: {}, 판매 종료 시각: {}, 남은 재고: {}, 스케줄링 시작 시각: {}",
                        ticketResponse.id(), ticketResponse.startSaleTime(), ticketResponse.endSaleTime(),
                        ticketResponse.remainStock(), scheduleTime);
            }
        } catch (JsonProcessingException | SchedulerException e) {
            log.error("Redis에 티켓 정보 업데이트 테스크 스케줄링 중 오류 발생", e);
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }
}
