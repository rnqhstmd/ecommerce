package com.loopers.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(RedisConnectionFactory connectionFactory) {
        Config config = new Config();

        // Spring이 관리하는 LettuceConnectionFactory에서 실제 연결 정보를 가져옴
        // Testcontainers 동적 포트, Master/Replica 구성 모두 호환
        String host = "localhost";
        int port = 6379;
        int database = 0;

        if (connectionFactory instanceof LettuceConnectionFactory lettuceFactory) {
            host = lettuceFactory.getHostName();
            port = lettuceFactory.getPort();
            database = lettuceFactory.getDatabase();
        }

        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database);

        return Redisson.create(config);
    }
}
