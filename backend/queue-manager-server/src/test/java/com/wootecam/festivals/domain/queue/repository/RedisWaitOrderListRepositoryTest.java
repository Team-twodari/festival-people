package com.wootecam.festivals.domain.queue.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.queue.dto.UpdateWaitOrder;
import com.wootecam.festivals.domain.queue.dto.WaitOrder;
import com.wootecam.festivals.domain.ticket.entity.TicketInfoWithId;
import com.wootecam.festivals.global.utils.TimeProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class RedisWaitOrderListRepositoryTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TimeProvider timeProvider;

    private RedisWaitOrderListRepository repository;

    private HashOperations<String, String, String> hashOperations;

    @BeforeEach
    void setUp() {
        repository = new RedisWaitOrderListRepository(redisTemplate, objectMapper, timeProvider);
        hashOperations = redisTemplate.opsForHash();
    }

    @Test
    @DisplayName("주어진 티켓 id에 해당하는 대기열 순번을 조회할 수 있다")
    void getAllIn_returnsUpdateWaitOrders() throws JsonProcessingException {
        // Given
        TicketInfoWithId ticket1 = new TicketInfoWithId(1L, null, null);
        List<TicketInfoWithId> tickets = List.of(ticket1);

        WaitOrder waitOrder = new WaitOrder(500, 10000L);
        String jsonValue = objectMapper.writeValueAsString(waitOrder);
        hashOperations.put(RedisWaitOrderListRepository.WAIT_ORDER_LIST_KEY, "tickets:1", jsonValue);

        // When
        List<UpdateWaitOrder> result = repository.getAllIn(tickets);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).ticketId()).isEqualTo(1L);
        assertThat(result.get(0).waitOrder()).isEqualTo(500);
        assertThat(result.get(0).updatedAt()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("주어진 티켓 id와 대기열 순번으로 갱신할 수 있다")
    void updateWaitOrderListBulk_updatesRedis() throws JsonProcessingException {
        // Given
        Map<Long, Integer> updates = new HashMap<>();
        updates.put(1L, 600);

        // When
        repository.updateWaitOrderListBulk(updates);

        // Then
        String storedValue = hashOperations.get(RedisWaitOrderListRepository.WAIT_ORDER_LIST_KEY, "tickets:1");
        assertNotNull(storedValue);
        WaitOrder storedOrder = objectMapper.readValue(storedValue, WaitOrder.class);
        assertEquals(600, storedOrder.waitOrder());
    }

    @Test
    @DisplayName("주어진 티켓 id가 없으면 대기열 순번을 갱신하지 않는다")
    void doesNotUpdateIfTicketIdNonExist() throws JsonProcessingException {
        // Given
        WaitOrder waitOrder = new WaitOrder(500, 10000L);
        String jsonValue = objectMapper.writeValueAsString(waitOrder);
        hashOperations.put(RedisWaitOrderListRepository.WAIT_ORDER_LIST_KEY, "tickets:1", jsonValue);

        // When
        repository.updateWaitOrderListBulk(Collections.EMPTY_MAP);

        // Then
        String storedValue = hashOperations.get(RedisWaitOrderListRepository.WAIT_ORDER_LIST_KEY, "tickets:1");
        assertNotNull(storedValue);
        WaitOrder storedOrder = objectMapper.readValue(storedValue, WaitOrder.class);
        assertEquals(500, storedOrder.waitOrder());
    }
}