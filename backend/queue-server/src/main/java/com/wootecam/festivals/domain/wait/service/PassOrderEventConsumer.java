package com.wootecam.festivals.domain.wait.service;

import static com.wootecam.festivals.domain.queue.constant.QueueRedisStreamConstants.PASS_ORDER_STREAM_GROUP;
import static com.wootecam.festivals.domain.queue.constant.QueueRedisStreamConstants.PASS_ORDER_STREAM_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.queue.dto.PassOrderMessage;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 대기열 갱신이 발생했을 때, PassOrderMessage를 수신하여 WebSocket을 통해 대기열 갱신 정보를 클라이언트에 전송하는 역할을 합니다. 이 클래스는 Redis Streams를 사용하여 메시지를
 * 수신하고 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PassOrderEventConsumer implements StreamListener<String, ObjectRecord<String, String>>, InitializingBean,
        DisposableBean {

    private final StringRedisTemplate redisTemplate;
    private final RedisStreamOperator redisOperator;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final WaitOrderService waitOrderService;

    private Subscription subscription;
    private StreamMessageListenerContainer<String, ObjectRecord<String, String>> container;

    /**
     * Redis Streams에서 PassOrderMessage를 수신하여 WebSocket을 통해 대기열 갱신 정보를 브로드캐스트 합니다.
     *
     * @param message 수신된 메시지
     */
    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        try {
            PassOrderMessage pass = objectMapper.readValue(message.getValue(), PassOrderMessage.class);
            log.info("Received PassOrderMessage: {}", pass);
            waitOrderService.removeWaiting(pass.ticketId(), pass.passOrder());
            messagingTemplate.convertAndSend("/topic/wait/" + pass.ticketId() + "/pass-order", pass.passOrder());
            redisTemplate.opsForStream().acknowledge(PASS_ORDER_STREAM_KEY, PASS_ORDER_STREAM_GROUP, message.getId());
        } catch (JsonProcessingException e) {
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.container = redisOperator.createStreamMessageListenerContainer();
        this.subscription = this.container.receive(
                Consumer.from(PASS_ORDER_STREAM_GROUP, "consumer-" + System.currentTimeMillis()),
                StreamOffset.create(PASS_ORDER_STREAM_KEY, ReadOffset.lastConsumed()),
                this
        );
        this.subscription.await(Duration.ofMillis(20));
        this.container.start();
    }

    @Override
    public void destroy() throws Exception {
        if (this.subscription != null) {
            this.subscription.cancel();
        }
        if (this.container != null) {
            this.container.stop();
        }
    }
}
