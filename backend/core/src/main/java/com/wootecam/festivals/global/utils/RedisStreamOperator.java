package com.wootecam.festivals.global.utils;

import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;
import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.connection.stream.StreamInfo.XInfoGroup;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisStreamOperator {

    private final StringRedisTemplate redisTemplate;

    public void createStreamConsumerGroup(String streamKey, String consumerGroupName) {
        // Stream이 존재 하지 않으면, MKSTREAM 옵션을 통해 만들고, ConsumerGroup또한 생성한다
        if (Boolean.FALSE.equals(this.redisTemplate.hasKey(streamKey))) {
            RedisAsyncCommands commands = (RedisAsyncCommands) this.redisTemplate
                    .getConnectionFactory()
                    .getConnection()
                    .getNativeConnection();

            CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8)
                    .add(CommandKeyword.CREATE)
                    .add(streamKey)
                    .add(consumerGroupName)
                    .add("0")
                    .add("MKSTREAM");

            commands.dispatch(CommandType.XGROUP, new StatusOutput(StringCodec.UTF8), args);
        }
        // Stream 존재시, ConsumerGroup 존재 여부 확인 후 ConsumerGroupd을 생성한다
        else {
            if (!isStreamConsumerGroupExist(streamKey, consumerGroupName)) {
                this.redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), consumerGroupName);
            }
        }
    }

    // ConsumerGroup 존재 여부 확인
    private boolean isStreamConsumerGroupExist(String streamKey, String consumerGroupName) {
        Iterator<XInfoGroup> iterator = this.redisTemplate
                .opsForStream().groups(streamKey).stream().iterator();

        while (iterator.hasNext()) {
            StreamInfo.XInfoGroup xInfoGroup = iterator.next();
            if (xInfoGroup.groupName().equals(consumerGroupName)) {
                return true;
            }
        }
        return false;
    }

    // RedisOperator :: 기본 StreamMessageListenerContainer 생성
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> createStreamMessageListenerContainer() {
        return StreamMessageListenerContainer.create(
                Objects.requireNonNull(this.redisTemplate.getConnectionFactory()),
                StreamMessageListenerContainer
                        .StreamMessageListenerContainerOptions.builder()
                        .targetType(String.class)
                        .pollTimeout(Duration.ofSeconds(20))
                        .batchSize(10)
                        .build()
        );
    }

    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> createStreamMessageListenerContainer(Integer pollTimeout,
                                                                                                                     Integer batchSize) {
        return StreamMessageListenerContainer.create(
                Objects.requireNonNull(this.redisTemplate.getConnectionFactory()),
                StreamMessageListenerContainer
                        .StreamMessageListenerContainerOptions.builder()
                        .targetType(String.class)
                        .pollTimeout(Duration.ofSeconds(pollTimeout))
                        .batchSize(batchSize)
                        .build()
        );
    }
}
