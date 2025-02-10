package com.wootecam.festivals.domain.payment.constant;

public class PaymentRedisStreamConstants {

    // 결제 요청을 전송하기 위한 Stream Key & Group
    public static final String PAYMENT_REQUEST_STREAM_KEY = "payment-request-stream";
    public static final String PAYMENT_REQUEST_STREAM_GROUP = "payment-request-stream-group";

    // 결제 결과를 전송하기 위한 Stream Key & Group
    public static final String PAYMENT_RESULT_STREAM_KEY = "payment-result-stream";
    public static final String PAYMENT_RESULT_STREAM_GROUP = "payment-result-stream-group";

    private PaymentRedisStreamConstants() {
    }
}
