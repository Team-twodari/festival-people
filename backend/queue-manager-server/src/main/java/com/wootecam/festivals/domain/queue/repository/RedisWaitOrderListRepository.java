package com.wootecam.festivals.domain.queue.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.queue.dto.UpdateWaitOrder;
import com.wootecam.festivals.domain.queue.dto.WaitOrder;
import com.wootecam.festivals.global.utils.TimeProvider;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 티켓 기반 대기열 시스템 정보를 Hash로 관리하는 redis 레포지토리
 * <p> field: ticket:{ticketId} </p>
 * <p> value: 현재 대기열 진입 가능 범위(min, max), 티켓 판매 시작/종료 시각, 대기열 진입 가능 범위 업데이트된 시각 </p>
 */
@Repository
public class RedisWaitOrderListRepository {

    public static final String WAIT_ORDER_LIST_KEY = "current_wait_order_list";

    private final HashOperations<String, String, String> hashOperations;

    private final ObjectMapper mapper;
    private final TimeProvider timeProvider;

    public RedisWaitOrderListRepository(RedisTemplate<String, String> redisTemplate,
                                        ObjectMapper mapper,
                                        TimeProvider timeProvider) {
        this.hashOperations = redisTemplate.opsForHash();
        this.mapper = mapper;
        this.timeProvider = timeProvider;
    }

    public List<UpdateWaitOrder> getAllWaitOrder() {
        Map<String, String> entries = hashOperations.entries(WAIT_ORDER_LIST_KEY);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        return entries.entrySet().stream()
                .map(entry -> {
                    try {
                        Long festivalId = parseFestivalIdFromWaitOrderListKey(entry.getKey());
                        WaitOrder waitOrder = mapper.readValue(entry.getValue(), WaitOrder.class);

                        return new UpdateWaitOrder(festivalId, waitOrder.waitOrder(), waitOrder.currentTime());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to parse wait order JSON", e);
                    }
                })
                .collect(Collectors.toList());
    }

    public void updateWaitOrderList(Long festivalId, Integer newWaitOrder) throws JsonProcessingException {
        String value = formatWaitOrderListValue(newWaitOrder);

        hashOperations.put(WAIT_ORDER_LIST_KEY, formatWaitOrderListKey(festivalId), value);
    }

    private String formatWaitOrderListKey(Long festivalId) {
        return "ticket:" + festivalId;
    }

    private Long parseFestivalIdFromWaitOrderListKey(String orderListKey) {
        return Long.parseLong(orderListKey.split(":")[1]);
    }

    private String formatWaitOrderListValue(Integer waitOrder) throws JsonProcessingException {
        long currentTime = timeProvider.getCurrentTimeInMilli();
        WaitOrder waitOrderDto = new WaitOrder(waitOrder, currentTime);

        return mapper.writeValueAsString(waitOrderDto);
    }
}
