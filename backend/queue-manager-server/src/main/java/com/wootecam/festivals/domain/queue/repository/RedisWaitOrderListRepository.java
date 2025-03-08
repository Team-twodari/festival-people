package com.wootecam.festivals.domain.queue.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.queue.dto.UpdateWaitOrder;
import com.wootecam.festivals.domain.queue.dto.WaitOrder;
import com.wootecam.festivals.domain.ticket.entity.TicketInfoWithId;
import com.wootecam.festivals.global.utils.TimeProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 티켓 기반 대기열 시스템 정보를 Hash로 관리하는 redis 레포지토리
 * <p> field: ticket:{ticketId} </p>
 * <p> value: 현재 대기열 진입 가능 범위(min, max), 티켓 판매 시작/종료 시각, 대기열 진입 가능 범위 업데이트된 시각 </p>
 */
@Repository
@Slf4j
public class RedisWaitOrderListRepository {

    public static final String WAIT_ORDER_LIST_KEY = "current_wait_order_list";

    private final RedisTemplate<String, String> redisTemplate;
    private final HashOperations<String, String, String> hashOperations;

    private final ObjectMapper mapper;
    private final TimeProvider timeProvider;

    public RedisWaitOrderListRepository(RedisTemplate<String, String> redisTemplate,
                                        ObjectMapper mapper,
                                        TimeProvider timeProvider) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
        this.mapper = mapper;
        this.timeProvider = timeProvider;
    }

    public List<UpdateWaitOrder> getAllIn(List<TicketInfoWithId> tickets) {
        if (tickets.isEmpty()) {
            return Collections.emptyList();
        }

        // 요청할 필드 리스트 생성
        List<String> ticketKeys = tickets.stream()
                .map(ticket -> formatField(ticket.ticketId()))
                .collect(Collectors.toList());

        // Redis에서 해당 필드만 조회
        List<String> values = hashOperations.multiGet(WAIT_ORDER_LIST_KEY, ticketKeys);

        List<UpdateWaitOrder> result = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);

            try {
                WaitOrder waitOrder = mapper.readValue(value, WaitOrder.class);
                Long ticketId = tickets.get(i).ticketId();
                result.add(new UpdateWaitOrder(ticketId, waitOrder.waitOrder(), waitOrder.currentTime()));
            } catch (JsonProcessingException e) {
                log.error("Failed to parse wait order JSON for ticket: {}", tickets.get(i).ticketId(), e);
            }
        }
        return result;
    }

    public void updateWaitOrderListBulk(Map<Long, Integer> updates) {
        // Serialize to Json
        Map<String, String> formattedUpdates = updates.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> formatField(entry.getKey()),
                        entry -> {
                            try {
                                return formatValue(entry.getValue());
                            } catch (JsonProcessingException e) {
                                log.error("Failed to serialize wait order JSON for ticket: {}", entry.getKey(), e);
                                return null;
                            }
                        }
                ));

        formattedUpdates.values().removeIf(Objects::isNull);

        if (formattedUpdates.isEmpty()) {
            log.warn("No valid wait orders to update in bulk");
            return;
        }

        List<Object> results = redisTemplate.executePipelined((RedisCallback<?>) (connection) -> {
            formattedUpdates.forEach((key, value) -> {
                try {
                    connection.hashCommands().hSet(WAIT_ORDER_LIST_KEY.getBytes(), key.getBytes(), value.getBytes());
                } catch (Exception e) {
                    log.error("Failed to update wait order: ticketKey={}, value={}", key, value, e);
                }
            });
            return null;
        });

        log.info("Bulk update completed. Success: {}, Total Requests: {}", results.size(), formattedUpdates.size());
    }

    private String formatField(Long ticketId) {
        return "tickets:" + ticketId;
    }

    private String formatValue(Integer waitOrder) throws JsonProcessingException {
        long currentTime = timeProvider.getCurrentTimeInMilli();
        WaitOrder waitOrderDto = new WaitOrder(waitOrder, currentTime);

        return mapper.writeValueAsString(waitOrderDto);
    }
}
