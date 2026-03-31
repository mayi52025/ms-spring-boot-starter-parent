package com.ms.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.ai.*;
import com.ms.middleware.cache.*;
import com.ms.middleware.cache.consistency.CacheConsistencyManager;
import com.ms.middleware.health.*;
import com.ms.middleware.lock.DistributedLock;
import com.ms.middleware.lock.RedisDistributedLock;
import com.ms.middleware.mq.MsMessageQueue;
import com.ms.middleware.mq.RabbitMessageQueue;
import com.ms.middleware.mq.idempotent.IdempotentStore;
import com.ms.middleware.mq.idempotent.RedisIdempotentStore;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * 中间件自动配置
 */
@Configuration
@EnableConfigurationProperties(MsMiddlewareProperties.class)
@AutoConfigureAfter(RedissonAutoConfiguration.class)
public class MsMiddlewareAutoConfiguration {

    private final MsMiddlewareProperties properties;

    public MsMiddlewareAutoConfiguration(MsMiddlewareProperties properties) {
        this.properties = properties;
    }

    // ==================== 缓存配置 ====================

    @Bean
    @ConditionalOnMissingBean(LocalCache.class)
    @ConditionalOnProperty(prefix = "ms.middleware.cache.local", name = "enabled", havingValue = "true")
    public LocalCache localCache() {
        return new LocalCache(properties.getCache().getLocal());
    }

    @Bean
    @ConditionalOnMissingBean(DistributedCache.class)
    @ConditionalOnProperty(prefix = "ms.middleware.cache.distributed", name = "enabled", havingValue = "true")
    public DistributedCache distributedCache(RedissonClient redissonClient) {
        return new DistributedCache(redissonClient, properties.getCache().getDistributed());
    }

    @Bean
    @ConditionalOnMissingBean(CacheConsistencyManager.class)
    @ConditionalOnProperty(prefix = "ms.middleware.cache.consistency", name = "enabled", havingValue = "true")
    public CacheConsistencyManager cacheConsistencyManager() {
        return new CacheConsistencyManager();
    }

    @Bean
    @ConditionalOnMissingBean(MultiLevelCache.class)
    public MultiLevelCache multiLevelCache(LocalCache localCache, 
                                          DistributedCache distributedCache, 
                                          CacheConsistencyManager cacheConsistencyManager) {
        MultiLevelCache multiLevelCache = new MultiLevelCache(localCache, distributedCache);
        
        if (properties.getCache().getConsistency().isEnabled() && 
            properties.getCache().getConsistency().isMultiLevelEnabled()) {
            multiLevelCache.setConsistencyManager(cacheConsistencyManager);
        }
        
        return multiLevelCache;
    }

    // ==================== 消息队列配置 ====================

    @Bean
    @ConditionalOnMissingBean(CachingConnectionFactory.class)
    @ConditionalOnProperty(prefix = "ms.middleware.mq", name = "enabled", havingValue = "true")
    public CachingConnectionFactory cachingConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(properties.getMq().getRabbit().getHost());
        factory.setPort(properties.getMq().getRabbit().getPort());
        factory.setUsername(properties.getMq().getRabbit().getUsername());
        factory.setPassword(properties.getMq().getRabbit().getPassword());
        factory.setVirtualHost(properties.getMq().getRabbit().getVirtualHost());
        factory.setConnectionTimeout(properties.getMq().getRabbit().getConnectionTimeout());
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(RabbitTemplate.class)
    @ConditionalOnProperty(prefix = "ms.middleware.mq", name = "enabled", havingValue = "true")
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange("ms-exchange");
        template.setRoutingKey("ms-routing-key");
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(IdempotentStore.class)
    @ConditionalOnProperty(prefix = "ms.middleware.mq.idempotent", name = "enabled", havingValue = "true")
    public IdempotentStore idempotentStore(RedissonClient redissonClient) {
        return new RedisIdempotentStore(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(MsMessageQueue.class)
    @ConditionalOnProperty(prefix = "ms.middleware.mq", name = "enabled", havingValue = "true")
    public MsMessageQueue msMessageQueue(RabbitTemplate rabbitTemplate, 
                                         CachingConnectionFactory connectionFactory, 
                                         ObjectMapper objectMapper, 
                                         IdempotentStore idempotentStore) {
        return new RabbitMessageQueue(rabbitTemplate, connectionFactory, objectMapper, idempotentStore);
    }

    // ==================== 故障自愈配置 ====================

    @Bean
    @ConditionalOnMissingBean(FaultSelfHealing.class)
    public FaultSelfHealing faultSelfHealing() {
        return FaultSelfHealing.getInstance();
    }

    @Bean
    @DependsOn({"redissonClient", "faultSelfHealing"})
    public RedisHealthChecker redisHealthChecker(RedissonClient redissonClient, FaultSelfHealing faultSelfHealing) {
        RedisHealthChecker checker = new RedisHealthChecker(redissonClient);
        // 注册Redis健康检查和恢复策略
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + properties.getRedis().getHost() + ":" + properties.getRedis().getPort())
              .setPassword(properties.getRedis().getPassword())
              .setDatabase(properties.getRedis().getDatabase());
        RedisRecoveryStrategy strategy = new RedisRecoveryStrategy(redissonClient, config);
        faultSelfHealing.registerComponent("Redis", checker, strategy);
        return checker;
    }

    @Bean
    @DependsOn({"cachingConnectionFactory", "faultSelfHealing"})
    @ConditionalOnProperty(prefix = "ms.middleware.mq", name = "enabled", havingValue = "true")
    public RabbitMQHealthChecker rabbitMQHealthChecker(CachingConnectionFactory connectionFactory, FaultSelfHealing faultSelfHealing) {
        RabbitMQHealthChecker checker = new RabbitMQHealthChecker(connectionFactory);
        // 注册RabbitMQ健康检查和恢复策略
        RabbitMQRecoveryStrategy strategy = new RabbitMQRecoveryStrategy(connectionFactory);
        faultSelfHealing.registerComponent("RabbitMQ", checker, strategy);
        return checker;
    }

    // ==================== AI热点识别配置 ====================

    @Bean
    @ConditionalOnMissingBean(HotKeyConfig.class)
    @ConditionalOnProperty(prefix = "ms.middleware.ai.hotKey", name = "enabled", havingValue = "true")
    public HotKeyConfig hotKeyConfig() {
        MsMiddlewareProperties.HotKeyProperties props = properties.getAi().getHotKey();
        return new HotKeyConfig(
            props.isEnabled(),
            props.getThreshold(),
            props.getTopN(),
            props.getStatisticsIntervalMs(),
            props.isAutoWarmup(),
            props.getExpireSeconds()
        );
    }

    @Bean
    @ConditionalOnMissingBean(HotKeyManager.class)
    @ConditionalOnProperty(prefix = "ms.middleware.ai.hotKey", name = "enabled", havingValue = "true")
    public HotKeyManager hotKeyManager(HotKeyConfig hotKeyConfig, MultiLevelCache multiLevelCache) {
        return new HotKeyManager(hotKeyConfig, multiLevelCache);
    }

    @Bean
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + properties.getRedis().getHost() + ":" + properties.getRedis().getPort())
              .setPassword(properties.getRedis().getPassword())
              .setDatabase(properties.getRedis().getDatabase());
        return org.redisson.Redisson.create(config);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // ==================== 分布式锁配置 ====================

    @Bean
    @ConditionalOnMissingBean(DistributedLock.class)
    public DistributedLock distributedLock(RedissonClient redissonClient) {
        return new RedisDistributedLock(redissonClient);
    }
}
