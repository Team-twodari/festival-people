package com.wootecam.festivals.domain.queue.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.queue.dto.WaitOrder;
import com.wootecam.festivals.global.utils.TimeProvider;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

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

    public void initializeWaitOrderList(Long ticketId)
            throws JsonProcessingException {
        String value = formatWaitOrderListValue(0);

        hashOperations.put(WAIT_ORDER_LIST_KEY, formatWaitOrderListKey(ticketId), value);
    }

    private String formatWaitOrderListKey(Long ticketId) {
        return "tickets:" + ticketId;
    }

    private String formatWaitOrderListValue(Integer newWaitOrder) throws JsonProcessingException {
        long currentTime = timeProvider.getCurrentTimeInMilli();
        WaitOrder waitOrderDto = new WaitOrder(newWaitOrder, currentTime);

        return mapper.writeValueAsString(waitOrderDto);
    }
}
