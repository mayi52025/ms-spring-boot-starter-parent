package com.ms.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.ai.*;
import com.ms.middleware.cache.*;
import com.ms.middleware.cache.consistency.CacheConsistencyManager;
import com.ms.middleware.circuit.CircuitBreaker;
import com.ms.middleware.circuit.Resilience4jCircuitBreaker;
import com.ms.middleware.health.*;
import com.ms.middleware.lock.DistributedLock;
import com.ms.middleware.lock.RedisDistributedLock;
import com.ms.middleware.metrics.MsMetrics;
import com.ms.middleware.mq.MsMessageQueue;
import com.ms.middleware.mq.RabbitMessageQueue;
import com.ms.middleware.mq.idempotent.IdempotentStore;
import com.ms.middleware.mq.idempotent.RedisIdempotentStore;
import com.ms.middleware.rate.RateLimiter;
import com.ms.middleware.rate.RedisRateLimiter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import com.ms.middleware.discovery.ServiceDiscoveryAutoConfiguration;
import com.ms.middleware.config.ConfigCenterAutoConfiguration;

/**
 * 中间件自动配置
 */
@Configuration
@EnableConfigurationProperties(MsMiddlewareProperties.class)
@AutoConfigureAfter(RedissonAutoConfiguration.class)
@Import({ServiceDiscoveryAutoConfiguration.class, ConfigCenterAutoConfiguration.class})
public class MsMiddlewareAutoConfiguration {

    private final MsMiddlewareProperties properties;

    public MsMiddlewareAutoConfiguration(MsMiddlewareProperties properties) {
        this.properties = properties;
    }

    // ==================== 缓存配置 ====================

    @Bean
    @ConditionalOnMissingBean(LocalCache.class)
    @ConditionalOnProperty(prefix = "ms.middleware.cache.local", name = "enabled", havingValue = "true")
    public LocalCache localCache(MsMetrics metrics) {
        return new LocalCache(properties.getCache().getLocal(), metrics);
    }

    @Bean
    @ConditionalOnMissingBean(DistributedCache.class)
    @ConditionalOnProperty(prefix = "ms.middleware.cache.distributed", name = "enabled", havingValue = "true")
    public DistributedCache distributedCache(RedissonClient redissonClient, MsMetrics metrics) {
        return new DistributedCache(redissonClient, properties.getCache().getDistributed(), properties, metrics);
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
    @ConditionalOnMissingBean(RabbitAdmin.class)
    @ConditionalOnProperty(prefix = "ms.middleware.mq", name = "enabled", havingValue = "true")
    public RabbitAdmin rabbitAdmin(CachingConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ms.middleware.mq", name = "enabled", havingValue = "true")
    public FanoutExchange defaultExchange() {
        return new FanoutExchange("ms-exchange", true, false);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ms.middleware.mq", name = "enabled", havingValue = "true")
    public Queue defaultQueue() {
        return new Queue("ms-queue", true, false, false);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ms.middleware.mq", name = "enabled", havingValue = "true")
    public Binding defaultBinding() {
        return BindingBuilder.bind(defaultQueue()).to(defaultExchange());
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
                                         IdempotentStore idempotentStore, 
                                         MsMetrics metrics, 
                                         RabbitAdmin rabbitAdmin) {
        return new RabbitMessageQueue(rabbitTemplate, connectionFactory, objectMapper, idempotentStore, properties, metrics, rabbitAdmin);
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
    public DistributedLock distributedLock(RedissonClient redissonClient, MsMetrics metrics) {
        return new RedisDistributedLock(redissonClient, metrics, properties);
    }

    // ==================== 限流配置 ====================

    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter rateLimiter(RedissonClient redissonClient, MsMetrics metrics) {
        return new RedisRateLimiter(redissonClient, metrics);
    }

    // ==================== 熔断配置 ====================

    @Bean
    @ConditionalOnMissingBean(CircuitBreaker.class)
    public CircuitBreaker circuitBreaker(MsMetrics metrics) {
        return new Resilience4jCircuitBreaker("ms-middleware-circuit-breaker", metrics);
    }
}
