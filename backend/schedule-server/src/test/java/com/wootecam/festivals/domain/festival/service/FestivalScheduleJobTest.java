package com.wootecam.festivals.domain.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.festival.entity.FestivalProgressStatus;
import com.wootecam.festivals.domain.festival.repository.FestivalRepository;
import com.wootecam.festivals.domain.member.entity.Member;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.utils.MemberRepository;
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

@SpringBootTest
@DisplayName("FestivalScheduleJob 클래스는")
class FestivalScheduleJobTest extends SpringBootTestConfig {
    @Autowired
    private FestivalScheduleJob festivalScheduleJob;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Festival testFestival;
    private Member admin;

    @BeforeEach
    void setUp() {
        clear();

        // Member 저장
        admin = memberRepository.save(Member.builder()
                .name("Test Organization")
                .email("test@example.com")
                .profileImg("test-profile-img")
                .build());

        // Festival 저장
        testFestival = festivalRepository.save(Festival.builder()
                .admin(admin) // 영속화된 Member 사용
                .title("테스트 축제")
                .description("축제 설명")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .festivalProgressStatus(FestivalProgressStatus.UPCOMING)
                .build());
    }


    @Test
    @DisplayName("JobDataMap에서 올바른 데이터를 읽어 상태를 변경한다.")
    void testExecute_ValidJobDataMap() {
        // Given
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("festivalId", testFestival.getId());
        jobDataMap.put("festivalStatus", FestivalProgressStatus.ONGOING.name());

        JobExecutionContext context = createJobExecutionContext(jobDataMap);

        // When
        festivalScheduleJob.execute(context);

        // Then
        Festival updatedFestival = festivalRepository.findById(testFestival.getId()).orElseThrow();
        assertThat(updatedFestival.getFestivalProgressStatus()).isEqualTo(FestivalProgressStatus.ONGOING);
    }

    @Test
    @DisplayName("JobDataMap에 잘못된 값이 있으면 ApiException을 던진다.")
    void testExecute_InvalidJobDataMap() {
        // Given
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("festivalId", null); // Invalid data
        jobDataMap.put("festivalStatus", FestivalProgressStatus.ONGOING.name());

        JobExecutionContext context = createJobExecutionContext(jobDataMap);

        // When & Then
        assertThatThrownBy(() -> festivalScheduleJob.execute(context))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("JobDataMap에서 festivalStatus 값이 없으면 ApiException을 던진다.")
    void testExecute_MissingFestivalStatus() {
        // Given
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("festivalId", testFestival.getId());
        // Missing festivalStatus

        JobExecutionContext context = createJobExecutionContext(jobDataMap);

        // When & Then
        assertThatThrownBy(() -> festivalScheduleJob.execute(context))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("JobDataMap에서 festivalId 값이 null이면 ApiException을 던진다.")
    void testExecute_MissingFestivalId() {
        // Given
        JobDataMap jobDataMap = new JobDataMap();
        // Missing festivalId
        jobDataMap.put("festivalStatus", FestivalProgressStatus.ONGOING.name());

        JobExecutionContext context = createJobExecutionContext(jobDataMap);

        // When & Then
        assertThatThrownBy(() -> festivalScheduleJob.execute(context))
                .isInstanceOf(ApiException.class);
    }

    /**
     * Helper method to create a JobExecutionContext with the given JobDataMap.
     */
    private JobExecutionContext createJobExecutionContext(JobDataMap jobDataMap) {
        JobDetail jobDetail = JobBuilder.newJob(FestivalScheduleJob.class)
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

        return new JobExecutionContextImpl(null, bundle, festivalScheduleJob);
    }
}
