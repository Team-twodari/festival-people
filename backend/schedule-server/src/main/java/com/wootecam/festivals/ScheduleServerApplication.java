package com.wootecam.festivals;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ScheduleServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScheduleServerApplication.class, args);
    }

}
