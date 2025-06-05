package com.wootecam.festivals.domain.queue.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wootecam.festivals.domain.queue.dto.UpdateWaitOrder;
import com.wootecam.festivals.domain.queue.repository.RedisWaitOrderListRepository;
import com.wootecam.festivals.domain.ticket.entity.TicketInfoWithId;
import com.wootecam.festivals.domain.ticket.repository.TicketInfoRedisRepository;
import com.wootecam.festivals.global.utils.TimeProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaitOrderUpdateServiceTest {

    @Mock
    private RedisWaitOrderListRepository waitOrderListRepository;

    @Mock
    private TicketInfoRedisRepository ticketInfoRedisRepository;

    @Mock
    private PassOrderEventProducer passOrderEventProducer;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private WaitOrderUpdateService waitOrderUpdateService;

    @BeforeEach
    void setUp() {
        when(timeProvider.getCurrentTimeInMilli()).thenReturn(10000L);
    }

    @Test
    @DisplayName("현재 판매 중인 티켓만 대기열 범위 갱신할 수 있다")
    void updateWaitOrders_updatesEligibleWaitOrders() {
        // Given
        TicketInfoWithId ticket1 = new TicketInfoWithId(1L, null, null);
        TicketInfoWithId ticket2 = new TicketInfoWithId(2L, null, null);
        List<TicketInfoWithId> tickets = List.of(ticket1, ticket2);

        UpdateWaitOrder waitOrder1 = new UpdateWaitOrder(1L, 500, 4000L);
        UpdateWaitOrder waitOrder2 = new UpdateWaitOrder(2L, 300, 6000L);
        List<UpdateWaitOrder> waitOrders = List.of(waitOrder1, waitOrder2);

        when(ticketInfoRedisRepository.getTicketsWithSaleStarted()).thenReturn(tickets);
        when(waitOrderListRepository.getAllIn(tickets)).thenReturn(waitOrders);

        // expects
        Map<Long, Integer> expectedUpdates = new HashMap<>();
        expectedUpdates.put(1L, 600); // only ticket1 should be updated

        // When
        waitOrderUpdateService.updateWaitOrders();

        // Then
        verify(waitOrderListRepository).updateWaitOrderListBulk(expectedUpdates);
    }

    @Test
    @DisplayName("현재 판매 중인 티켓이 없으면 대기열 범위를 갱신하지 않는다")
    void updateWaitOrders_doesNotUpdateIfNotEligible() {
        // Given
        TicketInfoWithId ticket1 = new TicketInfoWithId(1L, null, null);
        List<TicketInfoWithId> tickets = List.of(ticket1);

        UpdateWaitOrder waitOrder1 = new UpdateWaitOrder(1L, 500, 9000L); // Not eligible (9000L + 5000 > 10000L)
        List<UpdateWaitOrder> waitOrders = List.of(waitOrder1);

        when(ticketInfoRedisRepository.getTicketsWithSaleStarted()).thenReturn(tickets);
        when(waitOrderListRepository.getAllIn(tickets)).thenReturn(waitOrders);

        // When
        waitOrderUpdateService.updateWaitOrders();

        // Then
        verify(waitOrderListRepository, never()).updateWaitOrderListBulk(any());
    }
}
