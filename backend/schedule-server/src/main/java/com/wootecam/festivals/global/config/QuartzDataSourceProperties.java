package com.wootecam.festivals.global.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.org.quartz.datasource.schedule")
@Data
public class QuartzDataSourceProperties {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
}
