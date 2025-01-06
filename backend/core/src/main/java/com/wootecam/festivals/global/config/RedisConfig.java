package com.wootecam.festivals.global.config;

import static com.wootecam.festivals.domain.festival.constant.FestivalRedisStreamConstants.FESTIVAL_STREAM_GROUP;
import static com.wootecam.festivals.domain.festival.constant.FestivalRedisStreamConstants.FESTIVAL_STREAM_KEY;
import static com.wootecam.festivals.domain.ticket.constant.TicketRedisStreamConstants.TICKET_STREAM_GROUP;
import static com.wootecam.festivals.domain.ticket.constant.TicketRedisStreamConstants.TICKET_STREAM_KEY;

import com.wootecam.festivals.global.utils.RedisStreamOperator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("!test")
public class RedisConfig {

    private final RedisStreamOperator redisStreamOperator;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    public RedisConfig(RedisStreamOperator redisStreamOperator) {
        this.redisStreamOperator = redisStreamOperator;
    }

    @Bean
    @Profile("prod")
    public RedisConnectionFactory redisConnectionFactoryProd() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setPassword(redisPassword);

        return new LettuceConnectionFactory(redisConfig);
    }

    @Bean
    @Profile("local")
    public RedisConnectionFactory redisConnectionFactoryLocal() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        return new LettuceConnectionFactory(redisConfig);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    @PostConstruct
    public void initStream() {
        redisStreamOperator.createStreamConsumerGroup(FESTIVAL_STREAM_KEY, FESTIVAL_STREAM_GROUP);
        redisStreamOperator.createStreamConsumerGroup(TICKET_STREAM_KEY, TICKET_STREAM_GROUP);
    }
}
