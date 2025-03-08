package com.wootecam.festivals;

import com.wootecam.festivals.global.config.CloudConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(CloudConfiguration.class)
@EnableAsync
public class FestivalsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FestivalsApplication.class, args);
    }
}
