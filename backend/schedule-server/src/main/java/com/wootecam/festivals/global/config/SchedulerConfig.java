package com.wootecam.festivals.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.spi.JobFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final ApplicationContext applicationContext;

    /**
     * JobFactory 설정 Quartz에서 Job을 생성할 때 Spring의 ApplicationContext를 사용하기 위한 설정
     */
    @Bean
    public JobFactory jobFactory() {
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }
}
