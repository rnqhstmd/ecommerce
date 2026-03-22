package com.loopers.config;

import com.loopers.config.redis.RedisNodeInfo;
import com.loopers.config.redis.RedisProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.MasterSlaveServersConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties, RedisConnectionFactory connectionFactory) {
        Config config = new Config();

        // Testcontainers 등에서 동적으로 포트가 변경될 수 있으므로,
        // Spring이 관리하는 ConnectionFactory에서 실제 연결 정보를 가져옴
        String host = redisProperties.master().host();
        int port = redisProperties.master().port();
        int database = redisProperties.database();

        if (connectionFactory instanceof LettuceConnectionFactory lettuceFactory) {
            host = lettuceFactory.getHostName();
            port = lettuceFactory.getPort();
            database = lettuceFactory.getDatabase();
        }

        if (redisProperties.replicas() != null && !redisProperties.replicas().isEmpty()) {
            MasterSlaveServersConfig msConfig = config.useMasterSlaveServers()
                    .setMasterAddress("redis://" + host + ":" + port)
                    .setDatabase(database);
            for (RedisNodeInfo replica : redisProperties.replicas()) {
                msConfig.addSlaveAddress("redis://" + replica.host() + ":" + replica.port());
            }
        } else {
            config.useSingleServer()
                    .setAddress("redis://" + host + ":" + port)
                    .setDatabase(database);
        }

        return Redisson.create(config);
    }
}
