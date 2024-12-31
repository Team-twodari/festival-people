package com.wootecam.festivals.global.scheduler;

import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class PendingClaimScheduler {

    private static final String ERROR_COUNT_PREFIX = "errorCount:";
    private static final int MAX_ERROR_COUNT = 5;
    private static final int MAX_DELIVERY_COUNT = 2;
    private static final int MAX_NUMBER_FETCH = 10; // 한 번에 가져올 최대 메시지 수

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry; // Actuator MeterRegistry

    public void processPendingMessage(String streamKey, String groupName) {
        log.info("Processing pending messages for group: {}", groupName);

        try {
            // PENDING 상태의 메시지 가져오기
            PendingMessages pendingMessages = redisTemplate.opsForStream()
                    .pending(streamKey, groupName, Range.unbounded(), MAX_NUMBER_FETCH);
            for (PendingMessage pendingMessage : pendingMessages) {
                claimMessage(pendingMessage, streamKey, groupName);
                processMessage(pendingMessage, streamKey, groupName);
            }
        } catch (RuntimeException e) {
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "pending message 처리 중 에러 발생", e);
        }
    }

    private void claimMessage(PendingMessage pendingMessage, String streamKey, String groupName) {
        try {
            redisTemplate.opsForStream().claim(streamKey, groupName, pendingMessage.getIdAsString(),
                    Duration.ofMillis(20000)); // 20초 이상 대기한 메시지만 Claim
            log.info("Message {} has been claimed.", pendingMessage.getIdAsString());
        } catch (RuntimeException e) {
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "Stream claim 중 에러 발생", e);
        }
    }

    private void processMessage(PendingMessage pendingMessage, String streamKey, String groupName) {
        try {
            // 메시지 ID로 메시지 내용 조회
            List<ObjectRecord<String, String>> messagesToProcess = redisTemplate.opsForStream()
                    .range(String.class, streamKey, Range.just(pendingMessage.getIdAsString()));

            if (messagesToProcess.isEmpty()) {
                log.warn("Message {} is not present in the stream.", pendingMessage.getIdAsString());
                return;
            }

            ObjectRecord<String, String> message = messagesToProcess.get(0);

            // 최대 Delivery Count 초과 확인
            if (pendingMessage.getTotalDeliveryCount() > MAX_DELIVERY_COUNT) {
                log.warn("Message {} exceeded max delivery count. Logging and skipping ACK.",
                        pendingMessage.getIdAsString());
                logFailure(pendingMessage, "Max delivery count exceeded");
                return;
            }

            // 에러 횟수 확인
            String errorCountKey = ERROR_COUNT_PREFIX + pendingMessage.getIdAsString();
            int errorCount = redisTemplate.opsForValue().get(errorCountKey) == null ? 0
                    : Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get(errorCountKey)));
            if (errorCount >= MAX_ERROR_COUNT) {
                log.warn("Message {} exceeded max error count. Logging and skipping ACK.",
                        pendingMessage.getIdAsString());
                logFailure(pendingMessage, "Max error count exceeded");
                return;
            }

            // 메시지 처리 로직 (비즈니스 로직 추가)
            log.info("Processing message: {}", message);

            // 처리 완료 후 ACK
            redisTemplate.opsForStream().acknowledge(streamKey, groupName, pendingMessage.getIdAsString());
            log.info("Message {} has been successfully processed and acknowledged.", pendingMessage.getIdAsString());

        } catch (Exception e) {
            log.error("Error while processing message: {}. Logging without ACK.", pendingMessage.getIdAsString(), e);
            // 에러 발생 시 Redis에 에러 횟수 증가
            String errorCountKey = ERROR_COUNT_PREFIX + pendingMessage.getIdAsString();
            redisTemplate.opsForValue().increment(errorCountKey);
            logFailure(pendingMessage, "Processing exception: " + e.getMessage());

            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "메시지 처리 중 에러 발생", e);
        }
    }

    private void logFailure(PendingMessage pendingMessage, String reason) {
        log.error("Logging failed message: ID = {}, Reason = {}, DeliveryCount = {}",
                pendingMessage.getIdAsString(), reason, pendingMessage.getTotalDeliveryCount());

        // Actuator 메트릭에 실패 로그 기록
        meterRegistry.counter("festival.pending.failure", "reason", reason).increment();
    }
}
