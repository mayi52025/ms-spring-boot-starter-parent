# ms-spring-boot-starter

企业级 Spring Boot Starter，整合 Redis、Redisson、RabbitMQ，提供多级缓存自动降级与故障自愈机制、消息追踪与幂等消费、AI 热点识别等功能。

## 功能特性

### 1. 多级缓存
- **L1 本地缓存**：基于 Caffeine 实现，高性能本地缓存
- **L2 分布式缓存**：基于 Redis/Redisson 实现，支持分布式环境
- **自动降级**：当分布式缓存不可用时，自动降级到本地缓存
- **双写策略**：保证本地缓存和分布式缓存的一致性
- **TTL 管理**：支持设置缓存过期时间

### 2. 消息队列
- **RabbitMQ 集成**：支持 RabbitMQ 的各种特性
- **消息追踪**：记录消息的发送和消费过程
- **幂等消费**：防止重复消费消息
- **同步/异步发送**：支持同步和异步发送消息
- **延迟消息**：支持发送延迟消息
- **顺序消息**：支持发送顺序消息

### 3. 故障自愈
- **自动检测**：检测 Redis、RabbitMQ 等中间件的健康状态
- **自动恢复**：当中间件恢复时，自动重新连接
- **降级策略**：当中间件不可用时，使用降级策略

### 4. AI 热点识别
- **自动识别**：基于访问频率自动识别热点数据
- **智能缓存**：将热点数据缓存到本地，提高访问速度

### 5. 指标监控
- **Micrometer 集成**：集成 Micrometer 收集系统指标
- **监控指标**：缓存命中率、消息发送/消费数量、故障次数等

## 快速开始

### 1. 添加依赖

**Maven：**
```xml
<dependency>
    <groupId>com.ms</groupId>
    <artifactId>ms-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Gradle：**
```gradle
implementation 'com.ms:ms-spring-boot-starter:1.0.0-SNAPSHOT'
```

### 2. 启用中间件

在应用主类上添加 `@EnableMsMiddleware` 注解：

```java
@SpringBootApplication
@EnableMsMiddleware
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 配置中间件

在 `application.yml` 中添加配置：

```yaml
ms:
  middleware:
    # 缓存配置
    cache:
      local:
        enabled: true
        size: 1000
        ttl: 3600
      distributed:
        enabled: true
        ttl: 7200
      consistency:
        enabled: true
        multiLevelEnabled: true
    # Redis 配置
    redis:
      host: 192.168.100.102
      port: 6379
      password: 1234
      database: 0
    # 消息队列配置
    mq:
      enabled: true
      rabbit:
        host: 192.168.100.102
        port: 5672
        username: guest
        password: guest
        virtualHost: /
        connectionTimeout: 30000
        autoDeclare: true
      trace:
        enabled: true
        maxCacheSize: 10000
        expirationMinutes: 30
      idempotent:
        enabled: true
        prefix: "mq:idempotent:"
        expirationHours: 24
    # 故障自愈配置
    fault:
      enabled: true
      redis:
        checkInterval: 30
        maxAttempts: 5
      rabbit:
        checkInterval: 30
        maxAttempts: 5
    # AI 热点识别配置
    ai:
      hotSpot:
        enabled: true
        threshold: 10
        checkInterval: 60
    # 指标监控配置
    metrics:
      enabled: true
```

### 4. 使用缓存

```java
@Autowired
private MultiLevelCache multiLevelCache;

// 设置缓存
multiLevelCache.put("key", "value");

// 获取缓存
String value = multiLevelCache.get("key");

// 删除缓存
multiLevelCache.remove("key");

// 清除所有缓存
multiLevelCache.clear();

// 检查缓存是否存在
boolean exists = multiLevelCache.exists("key");
```

### 5. 使用消息队列

```java
@Autowired
private MsMessageQueue messageQueue;

// 发送消息
messageQueue.send("exchange", "routingKey", "message");

// 异步发送消息
messageQueue.sendAsync("exchange", "routingKey", "message");

// 发送延迟消息
messageQueue.sendDelayed("exchange", "routingKey", "message", 5000);

// 发送顺序消息
messageQueue.sendOrdered("exchange", "routingKey", "message", "orderKey");
```

### 6. 消费消息

```java
@Component
public class MessageConsumer {
    
    @RabbitListener(queues = "queueName")
    public void handleMessage(String message) {
        // 处理消息
        System.out.println("Received message: " + message);
    }
}
```
## 7. 使用分布式锁

```java
@Autowired
private DistributedLock distributedLock;

// 获取锁
boolean locked = distributedLock.lock("lock-key");
if (locked) {
    try {
        // 执行需要加锁的操作
    } finally {
        // 释放锁
        distributedLock.unlock("lock-key");
    }
}

// 获取锁，带过期时间
boolean lockedWithTimeout = distributedLock.lock("lock-key", 10, TimeUnit.SECONDS);

// 尝试获取锁
boolean tryLocked = distributedLock.tryLock("lock-key", 5, 10, TimeUnit.SECONDS);

// 检查锁是否存在
boolean exists = distributedLock.exists("lock-key");

// 获取锁的剩余过期时间
long remainingTime = distributedLock.getRemainingTime("lock-key", TimeUnit.SECONDS);
```
## 8. 使用限流功能

```java
@Autowired
private RateLimiter rateLimiter;

// 计数器限流：1分钟内最多10个请求
boolean allowed = rateLimiter.tryAcquire("user:123", 10, 1, TimeUnit.MINUTES);
if (allowed) {
    // 处理请求
} else {
    // 拒绝请求，返回限流提示
}

// 令牌桶算法限流：1分钟内最多10个请求，桶容量20
boolean allowedWithToken = rateLimiter.tryAcquireWithTokenBucket("user:123", 10, 1, TimeUnit.MINUTES, 20);

// 滑动窗口限流：1分钟内最多10个请求
boolean allowedWithSliding = rateLimiter.tryAcquireWithSlidingWindow("user:123", 10, 1, TimeUnit.MINUTES);

// 获取当前计数
long currentCount = rateLimiter.getCurrentCount("user:123");

// 重置限流计数
boolean reset = rateLimiter.reset("user:123");
```

## 配置说明

### 缓存配置
- **local.enabled**：是否启用本地缓存
- **local.size**：本地缓存大小
- **local.ttl**：本地缓存过期时间（秒）
- **distributed.enabled**：是否启用分布式缓存
- **distributed.ttl**：分布式缓存过期时间（秒）
- **consistency.enabled**：是否启用缓存一致性
- **consistency.multiLevelEnabled**：是否启用多级缓存一致性

### Redis 配置
- **host**：Redis 主机地址
- **port**：Redis 端口
- **password**：Redis 密码
- **database**：Redis 数据库

### 消息队列配置
- **enabled**：是否启用消息队列
- **rabbit.host**：RabbitMQ 主机地址
- **rabbit.port**：RabbitMQ 端口
- **rabbit.username**：RabbitMQ 用户名
- **rabbit.password**：RabbitMQ 密码
- **rabbit.virtualHost**：RabbitMQ 虚拟主机
- **rabbit.connectionTimeout**：RabbitMQ 连接超时（毫秒）
- **rabbit.autoDeclare**：是否自动声明队列
- **trace.enabled**：是否启用消息追踪
- **trace.maxCacheSize**：消息追踪缓存大小
- **trace.expirationMinutes**：消息追踪过期时间（分钟）
- **idempotent.enabled**：是否启用幂等消费
- **idempotent.prefix**：幂等键前缀
- **idempotent.expirationHours**：幂等键过期时间（小时）

### 故障自愈配置
- **enabled**：是否启用故障自愈
- **redis.checkInterval**：Redis 检查间隔（秒）
- **redis.maxAttempts**：Redis 最大尝试次数
- **rabbit.checkInterval**：RabbitMQ 检查间隔（秒）
- **rabbit.maxAttempts**：RabbitMQ 最大尝试次数

### AI 热点识别配置
- **hotSpot.enabled**：是否启用 AI 热点识别
- **hotSpot.threshold**：热点识别阈值
- **hotSpot.checkInterval**：热点检查间隔（秒）

### 指标监控配置
- **metrics.enabled**：是否启用指标监控

## 示例项目

请参考 `examples` 目录下的示例项目，了解如何在实际应用中使用本中间件。

## 故障排除

### 常见问题
1. **Redis 连接失败**：检查 Redis 配置是否正确，确保 Redis 服务正在运行。
2. **RabbitMQ 连接失败**：检查 RabbitMQ 配置是否正确，确保 RabbitMQ 服务正在运行。
3. **缓存不生效**：检查缓存配置是否正确，确保缓存功能已启用。
4. **消息发送失败**：检查消息队列配置是否正确，确保消息队列功能已启用。

### 日志说明
- 中间件的日志前缀为 `[ms-middleware]`，可以通过配置日志级别来查看详细信息。
- 故障自愈的日志前缀为 `[ms-fault-recovery]`，可以查看故障检测和恢复的详细信息。

## 版本历史

- **1.0.0-SNAPSHOT**：初始版本，包含多级缓存、消息队列、故障自愈、AI 热点识别等功能。

## 贡献指南

欢迎提交 Issue 和 Pull Request，帮助改进本中间件。

## 许可证

本项目采用 Apache 2.0 许可证。
