package com.wootecam.festivals.global.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TimeProviderTest {

    @Test
    void getCurrentTime_ReturnsFixedTime() {
        // Given
        TimeProvider timeProvider = Mockito.spy(new TimeProvider());
        LocalDateTime fixedTime = LocalDateTime.of(2025, 3, 8, 12, 0, 0);

        // Mocking: 특정 메서드의 결과를 고정
        doReturn(fixedTime).when(timeProvider).getCurrentTime();

        // When
        LocalDateTime result = timeProvider.getCurrentTime();

        // Then
        assertThat(result).isEqualTo(fixedTime);
    }

    @Test
    void getCurrentTimeInMilli_ReturnsFixedEpochMilli() {
        // Given
        TimeProvider timeProvider = Mockito.spy(new TimeProvider());
        Instant fixedInstant = Instant.ofEpochMilli(1736308800000L); // 2025-03-08 12:00:00 UTC

        // Mocking
        doReturn(fixedInstant.toEpochMilli()).when(timeProvider).getCurrentTimeInMilli();

        // When
        long result = timeProvider.getCurrentTimeInMilli();

        // Then
        assertThat(result).isEqualTo(1736308800000L);
    }
}
