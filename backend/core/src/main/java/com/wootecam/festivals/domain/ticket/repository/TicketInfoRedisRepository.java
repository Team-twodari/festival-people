package com.wootecam.festivals.domain.ticket.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.ticket.entity.TicketInfo;
import com.wootecam.festivals.domain.ticket.entity.TicketInfoWithId;
import com.wootecam.festivals.global.utils.TimeProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

/**
 * 티켓 정보를 관리하는 Repository
 * <p> Hash 로 구현되어 있으며 티켓 메타 데이터를 관리 </p>
 * <p> Hash Key: tickets_info
 * <p> Field: ticket:{ticketId} </p>
 * <p> Value: {@link TicketInfo} </p>
 */
@Repository
@Slf4j
public class TicketInfoRedisRepository extends RedisRepository {

    public static final String TICKET_INFO_KEY = "ticket_info";

    private final HashOperations<String, String, String> hashOperations;

    private final TimeProvider timeProvider;
    private final ObjectMapper mapper;

    public TicketInfoRedisRepository(RedisTemplate<String, String> redisTemplate, TimeProvider timeProvider,
                                     ObjectMapper mapper) {
        super(redisTemplate);
        this.hashOperations = redisTemplate.opsForHash();
        this.timeProvider = timeProvider;
        this.mapper = mapper;
    }

    /**
     * <p> 티켓 정보를 가져오는 메소드 </p>
     * <p> 존재하지 않는 티켓이라면 null 반환 </p>
     */
    public TicketInfo getTicketInfo(Long ticketId) {
        String value = hashOperations.get(TICKET_INFO_KEY, formatField(ticketId));
        if (value == null) {
            return null;
        }

        TicketInfo ticketInfo = null;
        try {
            ticketInfo = mapper.readValue(value, TicketInfo.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }

        return ticketInfo;
    }

    /**
     * <p> 현재 판매 중인 티켓 정보를 가져오는 메소드 </p>
     */
    public List<TicketInfoWithId> getTicketsWithSaleStarted() {
        LocalDateTime currentTime = timeProvider.getCurrentTime();

        // Redis에서 모든 티켓 정보 조회
        Map<String, String> entries = hashOperations.entries(TICKET_INFO_KEY);
        if (entries.isEmpty()) {
            return List.of();
        }

        return entries.entrySet().stream().map(entry -> {
            try {
                TicketInfo ticketInfo = mapper.readValue(entry.getValue(), TicketInfo.class);
                return isTicketOnSale(ticketInfo, currentTime)
                        ? createTicketInfoWithId(entry.getKey(), ticketInfo)
                        : null;
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize ticket info for key: {}", entry.getKey(), e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private TicketInfoWithId createTicketInfoWithId(String field, TicketInfo ticketInfo) {
        return new TicketInfoWithId(parseTicketId(field),
                ticketInfo.startSaleTime(),
                ticketInfo.endSaleTime());
    }

    private boolean isTicketOnSale(TicketInfo ticketInfo, LocalDateTime currentTime) {
        return (ticketInfo.startSaleTime().isBefore(currentTime) || ticketInfo.startSaleTime().isEqual(currentTime))
                && (ticketInfo.endSaleTime().isAfter(currentTime) || ticketInfo.endSaleTime().isEqual(currentTime));
    }

    /**
     * 티켓 정보를 설정하는 메소드
     */
    public void setTicketInfo(Long ticketId, LocalDateTime startSaleTime, LocalDateTime endSaleTime) {
        Assert.notNull(startSaleTime, "티켓 판매 시작 시각은 null 일 수 없습니다.");
        Assert.notNull(endSaleTime, "티켓 판매 종료 시각은 null 일 수 없습니다.");

        try {
            TicketInfo ticketInfo = new TicketInfo(startSaleTime, endSaleTime);
            String value = mapper.writeValueAsString(ticketInfo);

            hashOperations.put(TICKET_INFO_KEY, formatField(ticketId), value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String formatField(Long ticketId) {
        return "tickets:" + ticketId;
    }

    private Long parseTicketId(String field) {
        return Long.parseLong(field.split(":")[1]);
    }
}
