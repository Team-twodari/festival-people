package com.wootecam.festivals.domain.payment.service;

import com.wootecam.festivals.domain.payment.entity.PaymentStatus;
import com.wootecam.festivals.domain.payment.exception.PaymentException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentService {

    private static final Integer MAX_RETRY = 3;
    private static final long BASE_DELAY_MS = 500L; // 0.5초 기본 대기

    private final ExternalPaymentService externalPaymentService;
    private final ThreadPoolExecutor paymentExecutor = new ThreadPoolExecutor(
            10, 10, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(500),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public PaymentService(ExternalPaymentService externalPaymentService) {
        this.externalPaymentService = externalPaymentService;
    }

    /**
     * <p>결제를 처리하는 메서드</p>
     * <p>내부 결제 서버 문제로 실패하는 경우 최대 3회 재시도합니다.</p>
     * @param attempt 현재 재시도 횟수
     * @param paymentId 결제 ID
     * @return  결제 결과
     */
    public CompletableFuture<PaymentStatus> processPayment(int attempt, String paymentId) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return externalPaymentService.processPayment();
                    } catch (Exception e) {
                        throw new PaymentException(e);
                    }
                }, paymentExecutor)
                .thenCompose(status -> {
                    if (!status.isFailedByServer()) { // 성공하거나, 클라이언트 오류인 경우 재시도하지 않음
                        return CompletableFuture.completedFuture(status);
                    }

                    if (attempt == MAX_RETRY) { // 재시도 횟수를 모두 소진한 경우
                        log.error("재시도 {}회 실패 - paymentId: {}", MAX_RETRY, paymentId);
                        return CompletableFuture.completedFuture(status);
                    }

                    // 재시도 진행
                    long backoffTime = (long) (BASE_DELAY_MS * Math.pow(2, attempt - 1));
                    log.debug("서버 실패 재시도 ({}/{}) - 대기: {} ms", attempt, MAX_RETRY, backoffTime);

                    Executor delayedExec = CompletableFuture.delayedExecutor(backoffTime, TimeUnit.MILLISECONDS,
                            paymentExecutor);
                    return CompletableFuture.supplyAsync(() -> null, delayedExec)
                            .thenCompose(ignored -> processPayment(attempt + 1, paymentId));
                });
    }
}