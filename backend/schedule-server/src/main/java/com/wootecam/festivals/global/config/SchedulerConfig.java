package com.wootecam.festivals.global.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final ApplicationContext applicationContext;

    @Value("${spring.quartz.properties.org.quartz.dataSource.schedule.URL}")
    private String url;

    @Value("${spring.quartz.properties.org.quartz.dataSource.schedule.user}")
    private String username;

    @Value("${spring.quartz.properties.org.quartz.dataSource.schedule.password}")
    private String password;

    @Value("${spring.quartz.properties.org.quartz.dataSource.schedule.driver}")
    private String driverClassName;

    /**
     * JobFactory 설정 Quartz에서 Job을 생성할 때 Spring의 ApplicationContext를 사용하기 위한 설정
     */
    @Bean
    public JobFactory jobFactory() {
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    @Bean
    @QuartzDataSource
    public DataSource quartzDataSource() {
        log.info("Quartz DataSource URL: {}", url);
        log.info("Quartz DataSource Username: {}", username);
        log.info("Quartz DataSource Driver: {}", driverClassName);
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }
}
