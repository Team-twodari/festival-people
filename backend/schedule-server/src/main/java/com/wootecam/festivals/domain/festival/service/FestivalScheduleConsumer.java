package com.wootecam.festivals.domain.festival.service;

import static com.wootecam.festivals.domain.festival.constant.FestivalRedisStreamConstants.FESTIVAL_STREAM_GROUP;
import static com.wootecam.festivals.domain.festival.constant.FestivalRedisStreamConstants.FESTIVAL_STREAM_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.global.utils.RedisStreamOperator;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
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
@DependsOn("redisConnectionFactory")
@Slf4j
@RequiredArgsConstructor
public class FestivalScheduleConsumer implements StreamListener<String, ObjectRecord<String, String>>, InitializingBean,
        DisposableBean {

    private final StringRedisTemplate redisTemplate;
    private final FestivalSchedulerService festivalSchedulerService;
    private final ObjectMapper objectMapper;
    private final RedisStreamOperator redisOperator;
    private Subscription subscription;
    private StreamMessageListenerContainer<String, ObjectRecord<String, String>> container;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        log.info("Received message: {}", message);

        try {
            Festival festival = objectMapper.readValue(message.getValue(), Festival.class);
            festivalSchedulerService.scheduleStatusUpdate(festival);

            redisTemplate.opsForStream()
                    .acknowledge(FESTIVAL_STREAM_KEY, FESTIVAL_STREAM_GROUP, message.getId());
            log.info("성공적으로 메시지 처리 ACK : {}", message.getId());
        } catch (RuntimeException | JsonProcessingException e) {
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "축제 스트림 메시지 처리 중 예외 발생", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Starting FestivalScheduleConsumer...");
        // Consumer Group 설정
        this.redisOperator.createStreamConsumerGroup(FESTIVAL_STREAM_KEY, FESTIVAL_STREAM_GROUP);

        // StreamMessageListenerContainer 설정
        this.container = this.redisOperator.createStreamMessageListenerContainer();

        //Subscription 설정
        this.subscription = this.container.receive(
                Consumer.from(FESTIVAL_STREAM_GROUP, "consumer-" + System.currentTimeMillis()),
                StreamOffset.create(FESTIVAL_STREAM_KEY, ReadOffset.lastConsumed()),
                this
        );

        this.subscription.await(Duration.ofMillis(20));

        // redis listen 시작
        this.container.start();

        if (this.container.isRunning()) {
            log.info("FestivalScheduleConsumer is running...");
        }
    }

    @Override
    public void destroy() throws Exception {
        log.info("Closing FestivalScheduleConsumer...");
        if (this.subscription != null) {
            this.subscription.cancel();
        }
        if (this.container != null) {
            this.container.stop();
        }
    }
}
