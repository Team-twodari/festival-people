package com.wootecam.festivals.domain.payment.service;

import static com.wootecam.festivals.domain.payment.constant.PaymentRedisStreamConstants.PAYMENT_REQUEST_STREAM_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.payment.dto.PaymentRequest;
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
public class PaymentRequestEventProducer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void sendPaymentEvent(PaymentRequest paymentRequest) {
        log.info("Send payment event to redis: {}", paymentRequest);
        try {
            String paymentToJson = objectMapper.writeValueAsString(paymentRequest);

            ObjectRecord<String, String> message = StreamRecords.newRecord()
                    .ofObject(paymentToJson)
                    .withStreamKey(PAYMENT_REQUEST_STREAM_KEY);

            RecordId recordId = redisTemplate.opsForStream().add(message);

            if (recordId == null) {
                log.error("Failed to send payment event to stream");
                throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
            }

            log.info("Payment event sent to redis paymentId: {}", paymentRequest.paymentId());
        } catch (JsonProcessingException e) {
            log.error("Failed to send payment event to stream", e);
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }
}
