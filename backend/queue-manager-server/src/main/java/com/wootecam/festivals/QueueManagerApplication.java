package com.wootecam.festivals;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QueueManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueManagerApplication.class, args);
    }
}
