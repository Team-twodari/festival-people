package com.wootecam.festivals.domain.ticket.service;

import static com.wootecam.festivals.domain.ticket.service.TicketScheduleServiceTestFixture.createMembers;
import static com.wootecam.festivals.domain.ticket.service.TicketScheduleServiceTestFixture.createSaleOngoingTickets;
import static com.wootecam.festivals.domain.ticket.service.TicketScheduleServiceTestFixture.createSaleUpcomingTicketsAfterTenMinutes;
import static com.wootecam.festivals.domain.ticket.service.TicketScheduleServiceTestFixture.createSaleUpcomingTicketsExactlyTenMinutes;
import static com.wootecam.festivals.domain.ticket.service.TicketScheduleServiceTestFixture.createSaleUpcomingTicketsWithinTenMinutes;
import static com.wootecam.festivals.domain.ticket.service.TicketScheduleServiceTestFixture.createUpcomingFestival;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

import com.wootecam.festivals.domain.festival.dto.TicketResponse;
import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.festival.repository.FestivalRepository;
import com.wootecam.festivals.domain.member.entity.Member;
import com.wootecam.festivals.domain.ticket.entity.Ticket;
import com.wootecam.festivals.domain.ticket.entity.TicketInfo;
import com.wootecam.festivals.domain.ticket.entity.TicketStock;
import com.wootecam.festivals.domain.ticket.repository.CurrentTicketWaitRedisRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketInfoRedisRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketRepository;
import com.wootecam.festivals.utils.MemberRepository;
import com.wootecam.festivals.utils.SpringBootTestConfig;
import com.wootecam.festivals.utils.TicketStockRepository;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@DisplayName("TicketScheduleService 통합 테스트")
class TicketScheduleServiceTest extends SpringBootTestConfig {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TicketScheduleService ticketScheduleService;

    @Autowired
    private TicketInfoRedisRepository ticketInfoRedisRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TicketStockRepository ticketStockRepository;

    @Autowired
    private CurrentTicketWaitRedisRepository currentTicketWaitRedisRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private List<Ticket> saleUpcomingTicketsWithinTenMinutes;
    private List<Ticket> saleUpcomingTicketsAfterTenMinutes;
    private List<Ticket> saleOngoingTickets;
    private Festival festival;
    private int saleUpcomingTicketsWithinTenMinutesCount = 4;
    private int saleUpcomingTicketsAfterTenMinutesCount = 5;
    private int saleOngoingTicketsCount = 6;

    @BeforeEach
    void setUp() throws SchedulerException {
        clear();
        scheduler.clear();
        Member admin = createMembers(1).get(0);
        memberRepository.save(admin);

        festival = createUpcomingFestival(admin);
        festivalRepository.save(festival);

        saleUpcomingTicketsWithinTenMinutes = createSaleUpcomingTicketsWithinTenMinutes(
                saleUpcomingTicketsWithinTenMinutesCount, festival);
        saleUpcomingTicketsAfterTenMinutes = createSaleUpcomingTicketsAfterTenMinutes(
                saleUpcomingTicketsAfterTenMinutesCount, festival);
        saleOngoingTickets = createSaleOngoingTickets(saleOngoingTicketsCount, festival);

        ticketRepository.saveAll(saleUpcomingTicketsWithinTenMinutes);
        ticketRepository.saveAll(saleUpcomingTicketsAfterTenMinutes);
        ticketRepository.saveAll(saleOngoingTickets);

        List<TicketStock> saleUpcomingTicketStocksWithinTenMinutes = saleUpcomingTicketsWithinTenMinutes.stream()
                .flatMap(ticket -> ticket.createTicketStock().stream())
                .toList();
        List<TicketStock> saleUpcomingTicketStocksAfterTenMinutes = saleUpcomingTicketsAfterTenMinutes.stream()
                .flatMap(ticket -> ticket.createTicketStock().stream())
                .toList();
        List<TicketStock> saleOngoingTicketStocks = saleOngoingTickets.stream()
                .flatMap(ticket -> ticket.createTicketStock().stream())
                .toList();

        ticketStockRepository.saveAll(saleUpcomingTicketStocksWithinTenMinutes);
        ticketStockRepository.saveAll(saleUpcomingTicketStocksAfterTenMinutes);
        ticketStockRepository.saveAll(saleOngoingTicketStocks);
    }

    @Nested
    @DisplayName("scheduleRedisTicketInfoUpdate 메소드는")
    class DescribeScheduleRedisTicketInfoUpdate {

        @Test
        @DisplayName("10분 이내에 판매 시작되는 티켓은 Redis에 즉시 업데이트하고, 나머지는 스케줄링한다")
        void itUpdatesAndSchedulesTicketsCorrectly() {
            // When
            for (Ticket ticket : saleUpcomingTicketsWithinTenMinutes) {
                ticketScheduleService.scheduleRedisTicketInfoUpdate(TicketResponse.of(ticket));
            }

            for (Ticket ticket : saleUpcomingTicketsAfterTenMinutes) {
                ticketScheduleService.scheduleRedisTicketInfoUpdate(TicketResponse.of(ticket));
            }

            for (Ticket ticket : saleOngoingTickets) {
                ticketScheduleService.scheduleRedisTicketInfoUpdate(TicketResponse.of(ticket));
            }

            // Then
            await().untilAsserted(() -> {
                saleUpcomingTicketsWithinTenMinutes.forEach(ticket -> {
                    TicketInfo ticketInfo = ticketInfoRedisRepository.getTicketInfo(ticket.getId());
                    System.out.println("디버그" + ticketInfo);
                    assertThat(ticketInfo).isNotNull();
                    assertThat(ticketInfo.startSaleTime()).isCloseTo(ticket.getStartSaleTime(),
                            within(10, ChronoUnit.SECONDS));
                    assertThat(ticketInfo.endSaleTime()).isCloseTo(ticket.getEndSaleTime(),
                            within(10, ChronoUnit.SECONDS));
                });

                saleUpcomingTicketsAfterTenMinutes.forEach(ticket -> {
                    TicketInfo ticketInfo = ticketInfoRedisRepository.getTicketInfo(ticket.getId());
                    assertThat(ticketInfo).isNull();
                });

                List<Long> currentTicketWait = currentTicketWaitRedisRepository.getCurrentTicketWait();
                saleUpcomingTicketsWithinTenMinutes.forEach(ticket -> {
                    assertThat(currentTicketWait).contains(ticket.getId());
                });

                saleOngoingTickets.forEach(ticket -> {
                    TicketInfo ticketInfo = ticketInfoRedisRepository.getTicketInfo(ticket.getId());
                    assertThat(ticketInfo).isNotNull();
                    assertThat(ticketInfo.startSaleTime()).isCloseTo(ticket.getStartSaleTime(),
                            within(10, ChronoUnit.SECONDS));
                    assertThat(ticketInfo.endSaleTime()).isCloseTo(ticket.getEndSaleTime(),
                            within(10, ChronoUnit.SECONDS));
                });

                assertThat(scheduler.getTriggerKeys(GroupMatcher.anyGroup())).hasSize(
                        saleUpcomingTicketsAfterTenMinutesCount);
            });
        }

        @Test
        @DisplayName("정확히 10분 뒤에 판매 시작되는 티켓은 즉시 업데이트한다")
        void testUpdateTicketInfoImmediately() {
            // Given
            Ticket ticket = createSaleUpcomingTicketsExactlyTenMinutes(1, festival).get(0);
            ticketRepository.save(ticket);
            ticketStockRepository.saveAll(ticket.createTicketStock());

            // When
            ticketScheduleService.scheduleRedisTicketInfoUpdate(TicketResponse.of(ticket));

            // Then
            await().untilAsserted(() -> {
                TicketInfo ticketInfo = ticketInfoRedisRepository.getTicketInfo(ticket.getId());
                assertThat(ticketInfo).isNotNull();
                assertThat(ticketInfo.startSaleTime()).isCloseTo(ticket.getStartSaleTime(),
                        within(10, ChronoUnit.SECONDS));
                assertThat(ticketInfo.endSaleTime()).isCloseTo(ticket.getEndSaleTime(), within(10, ChronoUnit.SECONDS));
                List<Long> currentTicketWait = currentTicketWaitRedisRepository.getCurrentTicketWait();
                assertThat(currentTicketWait).contains(ticket.getId());
            });
        }
    }
}
