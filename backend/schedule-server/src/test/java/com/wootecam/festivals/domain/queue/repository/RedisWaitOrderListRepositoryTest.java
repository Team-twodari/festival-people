package com.wootecam.festivals.domain.queue.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.queue.dto.WaitOrder;
import com.wootecam.festivals.global.utils.TimeProvider;
import com.wootecam.festivals.utils.SpringBootTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class RedisWaitOrderListRepositoryTest extends SpringBootTestConfig {

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
    @DisplayName("주어진 ticketId의 대기열 순번을 0으로 초기화할 수 있다")
    void initializeWaitOrderList_storesInitialValueInRedis() throws JsonProcessingException {
        // Given
        Long ticketId = 1L;
        String expectedKey = "tickets:1";

        // When
        repository.initializeWaitOrderList(ticketId);

        // Then
        String storedValue = hashOperations.get(RedisWaitOrderListRepository.WAIT_ORDER_LIST_KEY, expectedKey);
        assertNotNull(storedValue);
        WaitOrder storedOrder = objectMapper.readValue(storedValue, WaitOrder.class);
        assertEquals(0, storedOrder.waitOrder());
    }
}