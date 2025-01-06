package com.wootecam.festivals.global.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.spi.JobFactory;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final QuartzDataSourceProperties quartzDataSourceProperties;
    private final ApplicationContext applicationContext;

    /**
     * Quartz 전용 데이터소스 설정
     */
    @QuartzDataSource
    public DataSource quartzDataSource() {
        log.info("Initializing Quartz DataSource with URL: {}", quartzDataSourceProperties.getUrl());

        return DataSourceBuilder.create()
                .url(quartzDataSourceProperties.getUrl())
                .username(quartzDataSourceProperties.getUsername())
                .password(quartzDataSourceProperties.getPassword())
                .driverClassName(quartzDataSourceProperties.getDriverClassName())
                .build();
    }

    /**
     * JobFactory 설정 Quartz에서 Job을 생성할 때 Spring의 ApplicationContext를 사용하기 위한 설정
     */
    @Bean
    public JobFactory jobFactory() {
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    /**
     * SchedulerFactoryBean 설정 Quartz 스케줄러 설정 JobFactory, DataSource 설정 기존 Job 덮어쓰기 설정
     *
     * @param quartzDataSource
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource quartzDataSource) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobFactory(jobFactory()); // JobFactory 지정
        factory.setDataSource(quartzDataSource); // Quartz 데이터소스 지정
        factory.setOverwriteExistingJobs(true); // 기존 Job 덮어쓰기 설정
        return factory;
    }
}
