package com.wootecam.festivals.domain.payment.service;

import static com.wootecam.festivals.domain.payment.constant.PaymentRedisStreamConstants.PAYMENT_RESULT_STREAM_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.payment.dto.PaymentResult;
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
public class PaymentResultEventProducer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void sendPaymentResultEvent(PaymentResult paymentResult) {
        log.info("Send payment result event to redis: {}", paymentResult);
        try {
            String paymentToJson = objectMapper.writeValueAsString(paymentResult);

            ObjectRecord<String, String> message = StreamRecords.newRecord()
                    .ofObject(paymentToJson)
                    .withStreamKey(PAYMENT_RESULT_STREAM_KEY);

            RecordId recordId = redisTemplate.opsForStream().add(message);

            if (recordId == null) {
                log.error("Failed to send payment result event to stream");
                throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
            }

            log.info("Payment result event sent to redis paymentId: {}", paymentResult.paymentId());
        } catch (JsonProcessingException e) {
            log.error("Failed to send payment result event to stream", e);
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }
}
