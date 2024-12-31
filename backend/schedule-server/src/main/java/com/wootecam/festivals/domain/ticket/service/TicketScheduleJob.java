package com.wootecam.festivals.domain.ticket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.festival.dto.TicketResponse;
import com.wootecam.festivals.domain.ticket.repository.CurrentTicketWaitRedisRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketInfoRedisRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketStockCountRedisRepository;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketScheduleJob implements Job {

    private final TicketInfoRedisRepository ticketInfoRedisRepository;
    private final TicketStockCountRedisRepository ticketStockCountRedisRepository;
    private final CurrentTicketWaitRedisRepository currentTicketWaitRedisRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        log.info("티켓 정보 업데이트 스케줄러 실행");
        try {
            String ticketToJson = jobExecutionContext.getJobDetail().getJobDataMap().getString("ticket");
            TicketResponse ticket = objectMapper.readValue(ticketToJson, TicketResponse.class);
            ticketInfoRedisRepository.setTicketInfo(ticket.id(), ticket.startSaleTime(), ticket.endSaleTime());
            ticketStockCountRedisRepository.setTicketStockCount(ticket.id(), ticket.remainStock());
            currentTicketWaitRedisRepository.addCurrentTicketWait(ticket.id());

            log.info("티켓 정보 업데이트 스케줄러 실행 완료 - 티켓 ID: {}, 판매 시작 시각: {}, 판매 종료 시각: {}, 남은 재고: {}", ticket.id(),
                    ticket.startSaleTime(), ticket.endSaleTime(), ticket.remainStock());
        } catch (RuntimeException | JsonProcessingException e) {
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "티켓 정보 업데이트 스케줄러 실행 중 오류 발생", e);
        }
    }
}
