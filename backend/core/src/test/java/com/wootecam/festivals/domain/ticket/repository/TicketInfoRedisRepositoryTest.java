package com.wootecam.festivals.domain.ticket.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.ticket.entity.TicketInfo;
import com.wootecam.festivals.domain.ticket.entity.TicketInfoWithId;
import com.wootecam.festivals.utils.TestApplication;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class TicketInfoRedisRepositoryTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private TicketInfoRedisRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(TicketInfoRedisRepository.TICKET_INFO_KEY);
    }

    @Test
    @DisplayName("티켓 정보를 조회할 수 있다")
    void getTicketInfo_Success() throws JsonProcessingException {
        // Given
        Long ticketId = 1L;
        LocalDateTime now = LocalDateTime.now();
        TicketInfo ticketInfo = new TicketInfo(now, now.plusHours(2));
        String jsonValue = mapper.writeValueAsString(ticketInfo);

        redisTemplate.opsForHash().put(TicketInfoRedisRepository.TICKET_INFO_KEY, "tickets:" + ticketId, jsonValue);

        // When
        TicketInfo result = repository.getTicketInfo(ticketId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.startSaleTime()).isEqualTo(now);
        assertThat(result.endSaleTime()).isEqualTo(now.plusHours(2));
    }

    @Test
    @DisplayName("티켓 정보가 없다면 null을 반환한다")
    void getTicketInfo_NotFound() {
        // Given
        Long ticketId = 2L;

        // When
        TicketInfo result = repository.getTicketInfo(ticketId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("현재 판매 중인 티켓들의 정보를 조회할 수 있다")
    void getTicketsWithSaleStarted_Success() throws JsonProcessingException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        TicketInfo ticketInfo1 = new TicketInfo(now.minusHours(1), now.plusHours(1));
        TicketInfo ticketInfo2 = new TicketInfo(now.minusHours(2), now.minusHours(1));
        String jsonValue1 = mapper.writeValueAsString(ticketInfo1);
        String jsonValue2 = mapper.writeValueAsString(ticketInfo2);

        redisTemplate.opsForHash().put(TicketInfoRedisRepository.TICKET_INFO_KEY, "tickets:1", jsonValue1);
        redisTemplate.opsForHash().put(TicketInfoRedisRepository.TICKET_INFO_KEY, "tickets:2", jsonValue2);

        // When
        List<TicketInfoWithId> result = repository.getTicketsWithSaleStarted();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).ticketId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("현재 판매 중인 티켓이 없다면 빈 리스트를 반환한다")
    void getTicketsWithSaleStarted_Empty() {
        // When
        List<TicketInfoWithId> result = repository.getTicketsWithSaleStarted();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("주어진 티켓 정보를 저장한다")
    void setTicketInfo_Success() throws JsonProcessingException {
        // Given
        Long ticketId = 1L;
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(2);

        // When
        repository.setTicketInfo(ticketId, start, end);

        // Then
        String storedValue = (String) redisTemplate.opsForHash()
                .get(TicketInfoRedisRepository.TICKET_INFO_KEY, "tickets:" + ticketId);
        assertThat(storedValue).isNotNull();

        TicketInfo ticketInfo = mapper.readValue(storedValue, TicketInfo.class);
        assertThat(ticketInfo.startSaleTime()).isEqualTo(start);
        assertThat(ticketInfo.endSaleTime()).isEqualTo(end);
    }

    @Test
    @DisplayName("티켓 정보가 제대로 주어지지 않았다면 예외를 반환한다")
    void setTicketInfo_JsonProcessingException() {
        // Given
        Long ticketId = 1L;
        LocalDateTime start = LocalDateTime.now();

        // When Then
        assertThrows(IllegalArgumentException.class, () -> repository.setTicketInfo(ticketId, start, null));
    }
}