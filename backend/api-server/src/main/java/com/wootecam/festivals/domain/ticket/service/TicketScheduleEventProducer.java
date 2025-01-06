package com.wootecam.festivals.domain.ticket.service;

import static com.wootecam.festivals.domain.ticket.constant.TicketRedisStreamConstants.TICKET_STREAM_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.festival.dto.TicketResponse;
import com.wootecam.festivals.domain.ticket.entity.Ticket;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketScheduleEventProducer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void sendEvent(Ticket ticket) {
        log.info("Send event to redis: {}", ticket);
        TicketResponse ticketResponse = TicketResponse.of(ticket);
        try {
            String ticketToJson = objectMapper.writeValueAsString(ticketResponse);

            ObjectRecord<String, String> message = StreamRecords.newRecord()
                    .ofObject(ticketToJson)
                    .withStreamKey(TICKET_STREAM_KEY);

            RecordId recordId = redisTemplate.opsForStream().add(message);

            if (recordId == null) {
                log.error("Failed to send ticket event to stream");
                throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
            }

            log.info("Event sent to redis ticketId: {}", ticket.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to send ticket event to stream", e);
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }
}
