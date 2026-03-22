package com.loopers.config;

import com.loopers.config.redis.RedisNodeInfo;
import com.loopers.config.redis.RedisProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.MasterSlaveServersConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();

        if (redisProperties.replicas() != null && !redisProperties.replicas().isEmpty()) {
            MasterSlaveServersConfig msConfig = config.useMasterSlaveServers()
                    .setMasterAddress("redis://" + redisProperties.master().host() + ":" + redisProperties.master().port())
                    .setDatabase(redisProperties.database());
            for (RedisNodeInfo replica : redisProperties.replicas()) {
                msConfig.addSlaveAddress("redis://" + replica.host() + ":" + replica.port());
            }
        } else {
            config.useSingleServer()
                    .setAddress("redis://" + redisProperties.master().host() + ":" + redisProperties.master().port())
                    .setDatabase(redisProperties.database());
        }

        return Redisson.create(config);
    }
}
