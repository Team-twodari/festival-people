package com.wootecam.festivals.domain.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.festival.dto.TicketResponse;
import com.wootecam.festivals.domain.ticket.repository.CurrentTicketWaitRedisRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketInfoRedisRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketStockCountRedisRepository;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.utils.SpringBootTestConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@DisplayName("TicketScheduleJob 클래스는")
class TicketScheduleJobTest extends SpringBootTestConfig {

    @Autowired
    private TicketScheduleJob ticketScheduleJob;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TicketInfoRedisRepository ticketInfoRedisRepository;

    @Autowired
    private TicketStockCountRedisRepository ticketStockCountRedisRepository;

    @Autowired
    private CurrentTicketWaitRedisRepository currentTicketWaitRedisRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private TicketResponse ticketResponse;

    @BeforeEach
    void setUp() {
        clear();

        ticketResponse = new TicketResponse(
                1L,
                "Test Ticket",
                "Test Detail",
                1000L,
                10,
                100L,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).minusHours(2),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("JobDataMap에서 올바른 데이터를 읽어 Redis에 저장한다.")
    void testExecute_ValidJobDataMap() throws Exception {
        // Given
        JobDataMap jobDataMap = new JobDataMap();
        String ticketJson = objectMapper.writeValueAsString(ticketResponse);
        jobDataMap.put("ticket", ticketJson);

        JobExecutionContext context = createJobExecutionContext(jobDataMap);

        // When
        ticketScheduleJob.execute(context);

        // Then
        assertThat(ticketInfoRedisRepository.getTicketInfo(ticketResponse.id())).isNotNull();
        assertThat(ticketStockCountRedisRepository.getTicketStockCount(ticketResponse.id())).isEqualTo(
                ticketResponse.remainStock());
        assertThat(currentTicketWaitRedisRepository.getCurrentTicketWait()).contains(ticketResponse.id());
    }

    @Test
    @DisplayName("JobDataMap에 잘못된 JSON 데이터를 제공하면 ApiException을 던진다.")
    void testExecute_InvalidJsonData() {
        // Given
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("ticket", "{invalid_json}");

        JobExecutionContext context = createJobExecutionContext(jobDataMap);

        // When & Then
        assertThatThrownBy(() -> ticketScheduleJob.execute(context))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("JobDataMap에 ticket 필드가 없으면 ApiException을 던진다.")
    void testExecute_MissingTicketField() {
        // Given
        JobDataMap jobDataMap = new JobDataMap();

        JobExecutionContext context = createJobExecutionContext(jobDataMap);

        // When & Then
        assertThatThrownBy(() -> ticketScheduleJob.execute(context))
                .isInstanceOf(ApiException.class);
    }

    private JobExecutionContext createJobExecutionContext(JobDataMap jobDataMap) {
        JobDetail jobDetail = JobBuilder.newJob(TicketScheduleJob.class)
                .withIdentity("testJob", "testGroup")
                .setJobData(jobDataMap)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("testTrigger", "testGroup")
                .startNow()
                .build();

        TriggerFiredBundle bundle = new TriggerFiredBundle(
                jobDetail,
                (OperableTrigger) trigger,
                null,
                false,
                null,
                null,
                null,
                null
        );

        return new JobExecutionContextImpl(null, bundle, ticketScheduleJob);
    }
}
