package com.wootecam.festivals.domain.ticket.service;

import static com.wootecam.festivals.domain.ticket.constant.TicketRedisStreamConstants.TICKET_STREAM_GROUP;
import static com.wootecam.festivals.domain.ticket.constant.TicketRedisStreamConstants.TICKET_STREAM_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.festival.dto.TicketResponse;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.global.utils.RedisStreamOperator;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TicketScheduleConsumer implements StreamListener<String, ObjectRecord<String, String>>, InitializingBean,
        DisposableBean {

    private final TicketScheduleService ticketScheduleService;
    private final StringRedisTemplate redisTemplate;
    private final RedisStreamOperator redisOperator;
    private final ObjectMapper objectMapper;
    private Subscription subscription;
    private StreamMessageListenerContainer<String, ObjectRecord<String, String>> container;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        log.info("Received message: {}", message);

        try {
            TicketResponse ticket = objectMapper.readValue(message.getValue(), TicketResponse.class);
            ticketScheduleService.scheduleRedisTicketInfoUpdate(ticket);

            redisTemplate.opsForStream().acknowledge(TICKET_STREAM_KEY, TICKET_STREAM_GROUP, message.getId());
            log.info("성공적으로 메시지 처리 ACK : {}", message.getId());
        } catch (JsonProcessingException e) {
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "티켓 스트림 메시지 처리 중 예외 발생", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Starting TicketScheduleConsumer...");
        // Consumer Group 설정
        this.redisOperator.createStreamConsumerGroup(TICKET_STREAM_KEY, TICKET_STREAM_GROUP);

        // StreamMessageListenerContainer 설정
        this.container = this.redisOperator.createStreamMessageListenerContainer();

        //Subscription 설정
        this.subscription = this.container.receive(
                Consumer.from(TICKET_STREAM_GROUP, "consumer-" + System.currentTimeMillis()),
                StreamOffset.create(TICKET_STREAM_KEY, ReadOffset.lastConsumed()),
                this
        );

        this.subscription.await(Duration.ofMillis(20));

        // redis listen 시작
        this.container.start();

        if (this.container.isRunning()) {
            log.info("TicketScheduleConsumer is running...");
        }
    }

    @Override
    public void destroy() throws Exception {
        log.info("Closing TicketScheduleConsumer...");
        if (this.subscription != null) {
            this.subscription.cancel();
        }
        if (this.container != null) {
            this.container.stop();
        }
    }
}
