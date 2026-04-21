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
import org.springframework.util.StringUtils;
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
import org.springframework.boot.ApplicationRunner;
import com.ms.middleware.discovery.ServiceDiscoveryAutoConfiguration;
import com.ms.middleware.config.ConfigCenterAutoConfiguration;
import java.util.concurrent.atomic.AtomicReference;

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
    public DistributedCache distributedCache(AtomicReference<RedissonClient> redissonClientRef, MsMetrics metrics) {
        return new DistributedCache(redissonClientRef, properties.getCache().getDistributed(), properties, metrics);
    }

    @Bean
    @ConditionalOnMissingBean(CacheConsistencyManager.class)
    @ConditionalOnProperty(prefix = "ms.middleware.cache.consistency", name = "enabled", havingValue = "true")
    public CacheConsistencyManager cacheConsistencyManager() {
        return new CacheConsistencyManager();
    }

    @Bean
    @ConditionalOnMissingBean(MultiLevelCache.class)
    @ConditionalOnProperty(prefix = "ms.middleware.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
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
    
    @Bean
    @ConditionalOnMissingBean(LocalCache.class)
    public LocalCache fallbackLocalCache(MsMetrics metrics) {
        // 创建一个默认的本地缓存，用于当所有缓存配置都被禁用时
        MsMiddlewareProperties.LocalCacheProperties localCacheProperties = new MsMiddlewareProperties.LocalCacheProperties();
        localCacheProperties.setEnabled(true);
        localCacheProperties.setSize(1000);
        localCacheProperties.setTtl(3600);
        return new LocalCache(localCacheProperties, metrics);
    }
    
    @Bean
    @ConditionalOnMissingBean(DistributedCache.class)
    public DistributedCache fallbackDistributedCache(AtomicReference<RedissonClient> redissonClientRef, MsMetrics metrics) {
        // 创建一个默认的分布式缓存，用于当所有缓存配置都被禁用时
        MsMiddlewareProperties.DistributedCacheProperties distributedCacheProperties = new MsMiddlewareProperties.DistributedCacheProperties();
        distributedCacheProperties.setEnabled(false);
        distributedCacheProperties.setTtl(7200);
        return new DistributedCache(redissonClientRef, distributedCacheProperties, properties, metrics);
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
    public ApplicationRunner rabbitMqPurgeOnStartup(RabbitAdmin rabbitAdmin) {
        return args -> {
            MsMiddlewareProperties.RabbitProperties rabbit = properties.getMq().getRabbit();
            if (!rabbit.isPurgeOnStartup()) {
                return;
            }
            if (rabbit.getPurgeQueues() == null || rabbit.getPurgeQueues().isEmpty()) {
                return;
            }
            for (String q : rabbit.getPurgeQueues()) {
                if (!StringUtils.hasText(q)) {
                    continue;
                }
                try {
                    rabbitAdmin.purgeQueue(q, true);
                } catch (Exception ignored) {
                    // 队列不存在/无权限等情况，开发环境下忽略即可
                }
            }
        };
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
    public IdempotentStore idempotentStore(AtomicReference<RedissonClient> redissonClientRef) {
        return new RedisIdempotentStore(redissonClientRef.get());
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
    @DependsOn({"redissonClientRef", "faultSelfHealing"})
    public RedisHealthChecker redisHealthChecker(AtomicReference<RedissonClient> redissonClientRef, FaultSelfHealing faultSelfHealing) {
        RedisHealthChecker checker = new RedisHealthChecker(redissonClientRef);
        // 注册Redis健康检查和恢复策略
        Config config = new Config();
        var singleServerConfig = config.useSingleServer()
              .setAddress("redis://" + properties.getRedis().getHost() + ":" + properties.getRedis().getPort())
              .setDatabase(properties.getRedis().getDatabase());
        if (StringUtils.hasText(properties.getRedis().getPassword())) {
            singleServerConfig.setPassword(properties.getRedis().getPassword());
        }
        RedisRecoveryStrategy strategy = new RedisRecoveryStrategy(redissonClientRef, config);
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
        var singleServerConfig = config.useSingleServer()
              .setAddress("redis://" + properties.getRedis().getHost() + ":" + properties.getRedis().getPort())
              .setDatabase(properties.getRedis().getDatabase());
        if (StringUtils.hasText(properties.getRedis().getPassword())) {
            singleServerConfig.setPassword(properties.getRedis().getPassword());
        }
        return org.redisson.Redisson.create(config);
    }

    @Bean
    public AtomicReference<RedissonClient> redissonClientRef(RedissonClient redissonClient) {
        return new AtomicReference<>(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // ==================== 分布式锁配置 ====================

    @Bean
    @ConditionalOnMissingBean(DistributedLock.class)
    public DistributedLock distributedLock(AtomicReference<RedissonClient> redissonClientRef, MsMetrics metrics) {
        return new RedisDistributedLock(redissonClientRef.get(), metrics, properties);
    }

    // ==================== 限流配置 ====================

    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter rateLimiter(AtomicReference<RedissonClient> redissonClientRef, MsMetrics metrics) {
        return new RedisRateLimiter(redissonClientRef.get(), metrics);
    }

    // ==================== 熔断配置 ====================

    @Bean
    @ConditionalOnMissingBean(CircuitBreaker.class)
    public CircuitBreaker circuitBreaker(MsMetrics metrics) {
        return new Resilience4jCircuitBreaker("ms-middleware-circuit-breaker", metrics);
    }
}
