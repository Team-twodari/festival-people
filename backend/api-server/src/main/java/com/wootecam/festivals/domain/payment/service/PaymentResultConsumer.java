package com.wootecam.festivals.domain.payment.service;

import static com.wootecam.festivals.domain.payment.constant.PaymentRedisStreamConstants.PAYMENT_RESULT_STREAM_GROUP;
import static com.wootecam.festivals.domain.payment.constant.PaymentRedisStreamConstants.PAYMENT_RESULT_STREAM_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.payment.dto.PaymentResult;
import com.wootecam.festivals.domain.payment.entity.Payment;
import com.wootecam.festivals.domain.payment.entity.PaymentStatus;
import com.wootecam.festivals.domain.payment.repository.PaymentRepository;
import com.wootecam.festivals.domain.purchase.entity.Purchase;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import java.time.Duration;
import java.util.Objects;
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
public class PaymentResultConsumer implements StreamListener<String, ObjectRecord<String, String>>,
        InitializingBean, DisposableBean {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private Subscription subscription;
    private StreamMessageListenerContainer<String, ObjectRecord<String, String>> container;

    private final PaymentResultService paymentResultService;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        log.debug("Received Payment Result Message: {}", message);

        try {
            PaymentResult paymentResult = objectMapper.readValue(message.getValue(), PaymentResult.class);

            String paymentId = paymentResult.paymentId();
            PaymentStatus status = paymentResult.status();

            paymentResultService.handlePaymentStatus(paymentId, status);

            log.debug("결제 후속 작업 완료: paymentId={}, status={}", paymentId, status);
        } catch (RuntimeException | JsonProcessingException e) {
            log.error("[onMessage] 결제 스트림 메시지 처리 중 예외 발생: {}", e.getMessage(), e);
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "결제 스트림 메시지 처리 중 예외 발생", e);
        }
    }

    /**
     * Bean 초기화 이후(프로퍼티 주입 완료 후)에 실행되는 메서드 Redis Stream Listener Container를 생성하고 구독(Subscription)을 시작한다.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Starting PaymentResultConsumer...");

        // 1) StreamMessageListenerContainer 생성
        this.container = StreamMessageListenerContainer.create(
                Objects.requireNonNull(this.redisTemplate.getConnectionFactory()),
                StreamMessageListenerContainer
                        .StreamMessageListenerContainerOptions.builder()
                        .targetType(String.class)
                        .pollTimeout(Duration.ofSeconds(5))
                        .batchSize(10)
                        .build()
        );

        // 2) Consumer 등록 & 구독 시작
        this.subscription = this.container.receive(
                Consumer.from(PAYMENT_RESULT_STREAM_GROUP, "consumer-" + System.currentTimeMillis()),
                StreamOffset.create(PAYMENT_RESULT_STREAM_KEY, ReadOffset.lastConsumed()),
                this
        );

        this.subscription.await(Duration.ofMillis(20));

        this.container.start();

        if (this.container.isRunning()) {
            log.info("PaymentResultConsumer is running...");
        }
    }

    /**
     * Bean 소멸 직전에 실행되는 메서드 구독(Subscription) 취소 및 컨테이너 정지
     */
    @Override
    public void destroy() throws Exception {
        log.info("Closing PaymentResultConsumer...");

        if (this.subscription != null) {
            this.subscription.cancel();
        }
        if (this.container != null) {
            this.container.stop();
        }
    }
}
