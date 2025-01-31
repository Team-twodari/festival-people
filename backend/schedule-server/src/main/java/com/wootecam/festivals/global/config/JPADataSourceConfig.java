package com.wootecam.festivals.global.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class JPADataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource jpaDataSource() {
        log.info("Initializing JPA DataSource with URL: {}", url);
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean
    public CommandLineRunner logEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        return args -> {
            String dataSource = entityManagerFactory.unwrap(SessionFactory.class).getSessionFactoryOptions()
                    .getServiceRegistry()
                    .getService(JdbcServices.class).getBootstrapJdbcConnectionAccess().obtainConnection().getMetaData()
                    .getURL();
            log.info("EntityManager is using DataSource URL: " + dataSource);
        };
    }

}
