package com.wootecam.festivals.domain.festival.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.festival.entity.FestivalProgressStatus;
import com.wootecam.festivals.domain.festival.repository.FestivalRepository;
import com.wootecam.festivals.domain.member.entity.Member;
import com.wootecam.festivals.utils.MemberRepository;
import com.wootecam.festivals.utils.SpringBootTestConfig;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("FestivalSchedulerService 클래스는 ")
class FestivalSchedulerServiceTest extends SpringBootTestConfig {

    @Autowired
    private FestivalSchedulerService festivalSchedulerService;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private Scheduler scheduler;

    private Member admin;

    @BeforeEach
    void setUp() throws SchedulerException {
        clear();
        scheduler.clear();

        admin = memberRepository.save(
                Member.builder()
                        .name("Test Admin")
                        .email("admin@test.com")
                        .profileImg("profile-img")
                        .build()
        );
    }

    @Test
    @DisplayName("페스티벌 시작 및 종료 시간을 스케줄링한다.")
    void testScheduleFestivals() throws SchedulerException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Festival festival = festivalRepository.save(Festival.builder()
                .admin(admin)
                .title("Test Festival")
                .description("Festival Description")
                .startTime(now.plusDays(10)) // 10초 뒤 시작
                .endTime(now.plusDays(20)) // 20초 뒤 종료
                .festivalProgressStatus(FestivalProgressStatus.UPCOMING)
                .build()
        );

        // When
        festivalSchedulerService.scheduleStatusUpdate(festival);

        // Then
        // 스케줄러에 등록된 작업 확인
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(
                JobKey.jobKey("festivalJob_" + festival.getId() + "_시작", "festivalGroup"));
        assertThat(triggers).isNotEmpty();
        assertThat(triggers.get(0).getStartTime()).isEqualTo(java.sql.Timestamp.valueOf(festival.getStartTime()));

        triggers = scheduler.getTriggersOfJob(
                JobKey.jobKey("festivalJob_" + festival.getId() + "_종료", "festivalGroup"));
        assertThat(triggers).isNotEmpty();
        assertThat(triggers.get(0).getStartTime()).isEqualTo(java.sql.Timestamp.valueOf(festival.getEndTime()));
    }

    @Test
    @DisplayName("이미 완료된 축제는 즉시 상태를 COMPLETED로 업데이트한다.")
    void testImmediateExecutionForPastFestival() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Festival pastFestival = festivalRepository.save(Festival.builder()
                .admin(admin)
                .title("Past Festival")
                .description("Festival Description")
                .startTime(now)
                .endTime(now.plusSeconds(5))
                .festivalProgressStatus(FestivalProgressStatus.UPCOMING)
                .build()
        );

        // When
        festivalSchedulerService.scheduleStatusUpdate(pastFestival);

        // Then
        // 바로 상태가 업데이트되었는지 확인
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Festival updatedFestival = festivalRepository.findById(pastFestival.getId()).orElseThrow();
            assertThat(updatedFestival.getFestivalProgressStatus()).isEqualTo(FestivalProgressStatus.COMPLETED);
        });

        // 완료된 축제에 대해 작업이 스케줄링되어 있지 않은지 확인
        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertThat(scheduler.getJobKeys(GroupMatcher.anyGroup())).isEmpty();
        });
    }

    @Test
    @DisplayName("서버 재시작 시 모든 축제를 스케줄링한다.")
    void testScheduleAllFestivals() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        // 다가오는 축제
        Festival upcomingFestival = Festival.builder()
                .admin(admin)
                .title("다가오는 축제")
                .description("축제 설명")
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(7))
                .build();
        // 진행중인 축제
        Festival ongoingFestival = Festival.builder()
                .admin(admin)
                .title("진행중인 축제")
                .description("축제 설명")
                .startTime(now)
                .endTime(now.plusDays(1))
                .build();
        // 종료된 축제
        Festival completedFestival = Festival.builder()
                .admin(admin)
                .title("종료된 축제")
                .description("축제 설명")
                .startTime(now)
                .endTime(now)
                .build();

        memberRepository.save(admin);
        festivalRepository.saveAll(Arrays.asList(upcomingFestival, ongoingFestival, completedFestival));

        // When
        festivalSchedulerService.scheduleStatusUpdate(upcomingFestival);
        festivalSchedulerService.scheduleStatusUpdate(ongoingFestival);
        festivalSchedulerService.scheduleStatusUpdate(completedFestival);

        // Then
        // 종료된 축제 상태 업데이트 확인
        await().atMost(2, SECONDS).untilAsserted(() -> {
            Festival updatedCompletedFestival = festivalRepository.findById(completedFestival.getId()).orElseThrow();
            assertThat(updatedCompletedFestival.getFestivalProgressStatus()).isEqualTo(
                    FestivalProgressStatus.COMPLETED);
        });

        // 다가오는 축제 스케줄 확인
        await().atMost(2, SECONDS).untilAsserted(() -> {
            List<? extends Trigger> upcomingTriggers = scheduler.getTriggersOfJob(
                    JobKey.jobKey("festivalJob_" + upcomingFestival.getId() + "_시작", "festivalGroup"));
            assertThat(upcomingTriggers).isNotEmpty();
        });

        // 진행중인 축제 스케줄 확인
        await().atMost(2, SECONDS).untilAsserted(() -> {
            List<? extends Trigger> ongoingTriggers = scheduler.getTriggersOfJob(
                    JobKey.jobKey("festivalJob_" + ongoingFestival.getId() + "_종료", "festivalGroup"));
            assertThat(ongoingTriggers).isNotEmpty();
        });
    }

    @Test
    @DisplayName("이미 등록된 Job을 다시 등록하지 않는다.")
    void testAvoidDuplicateJobRegistration() throws SchedulerException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Festival festival = festivalRepository.save(Festival.builder()
                .admin(admin)
                .title("Test Festival")
                .description("Festival Description")
                .startTime(now.plusDays(10))
                .endTime(now.plusDays(20))
                .festivalProgressStatus(FestivalProgressStatus.UPCOMING)
                .build()
        );

        // When
        festivalSchedulerService.scheduleStatusUpdate(festival);
        festivalSchedulerService.scheduleStatusUpdate(festival);

        // Then
        assertThat(scheduler.getJobKeys(GroupMatcher.jobGroupEquals("festivalGroup")))
                .hasSize(2); // 시작이랑 종료 두 개
    }

    @Test
    @DisplayName("등록되지 않은 Job은 성공적으로 등록된다.")
    void testNewJobRegistration() throws SchedulerException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Festival festival = festivalRepository.save(Festival.builder()
                .admin(admin)
                .title("Test Festival")
                .description("Festival Description")
                .startTime(now.plusDays(10))
                .endTime(now.plusDays(20))
                .festivalProgressStatus(FestivalProgressStatus.UPCOMING)
                .build()
        );

        // When
        festivalSchedulerService.scheduleStatusUpdate(festival);

        // Then
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(
                JobKey.jobKey("festivalJob_" + festival.getId() + "_시작", "festivalGroup"));
        assertThat(triggers).isNotEmpty();
        assertThat(triggers.get(0).getStartTime()).isEqualTo(java.sql.Timestamp.valueOf(festival.getStartTime()));
    }

    @Test
    @DisplayName("Job 실행 중 Exception이 발생하면 상태를 유지한다.")
    void testJobExecutionWithException() throws SchedulerException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Festival festival = festivalRepository.save(Festival.builder()
                .admin(admin)
                .title("Error Festival")
                .description("This festival will trigger an error")
                .startTime(now.plusSeconds(5))
                .endTime(now.plusSeconds(10))
                .festivalProgressStatus(FestivalProgressStatus.UPCOMING)
                .build()
        );

        // Mock 예외를 발생시키는 Job 등록
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("festivalTrigger_" + festival.getId() + "error" + "_시작", "festivalGroup")
                .startNow()
                .build();

        scheduler.scheduleJob(
                JobBuilder.newJob(FestivalScheduleJob.class)
                        .withIdentity("festivalJob_" + festival.getId() + "error" + "_시작", "festivalGroup")
                        .storeDurably(false)
                        .build(),
                trigger
        );

        // When
        festivalSchedulerService.scheduleStatusUpdate(festival);

        // Then
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(
                JobKey.jobKey("festivalJob_" + festival.getId() + "_시작", "festivalGroup"));
        assertThat(triggers).isNotEmpty(); // Job은 여전히 스케줄러에 남아있음
    }
}
