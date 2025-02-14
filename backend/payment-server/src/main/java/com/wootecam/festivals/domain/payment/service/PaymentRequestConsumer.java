package com.wootecam.festivals.domain.payment.service;

import static com.wootecam.festivals.domain.payment.constant.PaymentRedisStreamConstants.PAYMENT_REQUEST_STREAM_GROUP;
import static com.wootecam.festivals.domain.payment.constant.PaymentRedisStreamConstants.PAYMENT_REQUEST_STREAM_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.payment.dto.PaymentRequest;
import com.wootecam.festivals.domain.payment.dto.PaymentResult;
import com.wootecam.festivals.domain.payment.entity.PaymentStatus;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.global.utils.RedisStreamOperator;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

@Slf4j
@Component
@DependsOn(value = {"redisConnectionFactory", "redisStreamInitializer"})
@RequiredArgsConstructor
public class PaymentRequestConsumer implements StreamListener<String, ObjectRecord<String, String>>,
        InitializingBean, DisposableBean {

    private final StringRedisTemplate redisTemplate;
    private final RedisStreamOperator redisStreamOperator;
    private final ObjectMapper objectMapper;

    private final PaymentService paymentService;
    private final PaymentResultEventProducer paymentResultEventProducer;

    private Subscription subscription;
    private StreamMessageListenerContainer<String, ObjectRecord<String, String>> container;

    private final ThreadPoolExecutor paymentExecutor = new ThreadPoolExecutor(
            10, 10, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(500),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        log.debug("Received Payment Message: {}", message);

        try {
            PaymentRequest paymentRequest = objectMapper.readValue(message.getValue(), PaymentRequest.class);

            String paymentId = paymentRequest.paymentId();

            paymentService.processPayment(0, paymentId)
                    .thenAcceptAsync(status -> handlePaymentResult(status, paymentId), paymentExecutor)
                    .thenRunAsync(() -> {
                        redisTemplate.opsForStream()
                                .acknowledge(PAYMENT_REQUEST_STREAM_KEY, PAYMENT_REQUEST_STREAM_GROUP, message.getId());
                        log.info("결제 요청 메시지 처리 완료: messageId {}, paymentId {}", message.getId(), paymentId);
                    }, paymentExecutor)
                    .exceptionally(e -> {
                                log.error("결제 서버에 장애가 발생하였습니다. paymentId : {}", paymentId);
                                return null;
                            }
                    );
        } catch (RuntimeException | JsonProcessingException e) {
            log.error("[onMessage] 결제 스트림 메시지 처리 중 예외 발생: {}", e.getMessage(), e);
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "결제 스트림 메시지 처리 중 예외 발생", e);
        }
    }

    private void handlePaymentResult(PaymentStatus status, String paymentId) {
        log.debug("결제 완료 - paymentId {} status {}", paymentId, status);
        paymentResultEventProducer.sendPaymentResultEvent(new PaymentResult(paymentId, status));
    }

    /**
     * Bean 초기화 이후(프로퍼티 주입 완료 후)에 실행되는 메서드 Redis Stream Listener Container를 생성하고 구독(Subscription)을 시작한다.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Starting PaymentRequestConsumer...");

        // 1) StreamMessageListenerContainer 생성
        this.container = redisStreamOperator.createStreamMessageListenerContainer(5, 10);

        // 2) Consumer 등록 & 구독 시작
        this.subscription = this.container.receive(
                Consumer.from(PAYMENT_REQUEST_STREAM_GROUP, "consumer-" + System.currentTimeMillis()),
                StreamOffset.create(PAYMENT_REQUEST_STREAM_KEY, ReadOffset.lastConsumed()),
                this
        );

        this.subscription.await(Duration.ofMillis(20));

        this.container.start();

        if (this.container.isRunning()) {
            log.info("PaymentRequestConsumer is running...");
        }
    }

    /**
     * Bean 소멸 직전에 실행되는 메서드 구독(Subscription) 취소 및 컨테이너 정지
     */
    @Override
    public void destroy() throws Exception {
        log.info("Closing PaymentRequestConsumer...");

        if (this.subscription != null) {
            this.subscription.cancel();
        }
        if (this.container != null) {
            this.container.stop();
        }
    }
}
