package com.ms.middleware;

import com.ms.middleware.cache.DistributedCache;
import com.ms.middleware.cache.LocalCache;
import com.ms.middleware.cache.MultiLevelCache;
import com.ms.middleware.cache.MsCache;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 中间件自动配置类
 * 自动注册缓存、消息队列等中间件组件
 */
@Configuration
@EnableConfigurationProperties(MsMiddlewareProperties.class)
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
public class MsMiddlewareAutoConfiguration {

    private final MsMiddlewareProperties properties;

    public MsMiddlewareAutoConfiguration(MsMiddlewareProperties properties) {
        this.properties = properties;
    }

    /**
     * 本地缓存 Bean
     */
    @Bean
    @ConditionalOnMissingBean(LocalCache.class)
    public LocalCache localCache() {
        return new LocalCache(properties.getCache().getLocal());
    }

    /**
     * Redisson 客户端 Bean
     */
    @Bean
    @ConditionalOnProperty(prefix = "ms.middleware.cache.distributed", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 从配置中读取 Redis 连接信息
        // 注意：这里简化处理，实际项目中应该从 Spring 环境中读取完整的 Redis 配置
        config.useSingleServer()
                .setAddress("redis://192.168.100.102:6379")
                .setPassword("1234")
                .setDatabase(0);
        
        return Redisson.create(config);
    }

    /**
     * 分布式缓存 Bean
     */
    @Bean
    @ConditionalOnProperty(prefix = "ms.middleware.cache.distributed", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(DistributedCache.class)
    public DistributedCache distributedCache(RedissonClient redissonClient) {
        return new DistributedCache(redissonClient, properties.getCache().getDistributed());
    }

    /**
     * 多级缓存 Bean
     */
    @Bean
    @ConditionalOnMissingBean(MultiLevelCache.class)
    public MultiLevelCache multiLevelCache(LocalCache localCache, DistributedCache distributedCache) {
        return new MultiLevelCache(localCache, distributedCache);
    }

    /**
     * 缓存接口 Bean（默认使用多级缓存）
     */
    @Bean
    @ConditionalOnMissingBean(MsCache.class)
    public MsCache msCache(MultiLevelCache multiLevelCache) {
        return multiLevelCache;
    }

    /**
     * 开发环境配置
     */
    @Configuration
    @Profile("dev")
    public static class DevConfiguration {
        // 开发环境特定配置
    }

    /**
     * 生产环境配置
     */
    @Configuration
    @Profile("prod")
    public static class ProdConfiguration {
        // 生产环境特定配置
    }
}
