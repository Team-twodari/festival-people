package com.wootecam.festivals.global.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class TimeProvider {

    public static final ZoneOffset KTC_ZONE = ZoneOffset.of("+9");

    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    public long getCurrentTimeInMilli() {
        return Instant.now().toEpochMilli();
    }
}
