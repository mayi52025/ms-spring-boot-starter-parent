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
## 9. 使用熔断功能

```java
@Autowired
private CircuitBreaker circuitBreaker;

// 执行受熔断保护的操作
try {
    String result = circuitBreaker.execute(() -> {
        // 调用依赖服务
        return dependencyService.call();
    });
    // 处理结果
} catch (Exception e) {
    // 处理异常
}

// 执行受熔断保护的操作，带 fallback
String result = circuitBreaker.execute(() -> {
    // 调用依赖服务
    return dependencyService.call();
}, () -> {
    // 熔断时的 fallback 操作
    return "Fallback result";
});

// 执行受熔断保护的操作，带异常处理
String result = circuitBreaker.executeWithExceptionHandling(() -> {
    // 调用依赖服务
    return dependencyService.call();
});

// 获取熔断状态
CircuitBreaker.CircuitState state = circuitBreaker.getState();
if (state == CircuitBreaker.CircuitState.OPEN) {
    // 熔断状态，服务不可用
} else if (state == CircuitBreaker.CircuitState.HALF_OPEN) {
    // 半开状态，尝试恢复
} else {
    // 关闭状态，服务正常
}

// 重置熔断状态
circuitBreaker.reset();
```
## 10. 安全功能

### 10.1 安全功能概述

本模块提供了以下安全功能：

- **分布式缓存和锁的访问控制**：通过安全的键生成机制，防止未授权访问
- **消息队列的消息加密**：使用 AES 加密算法对消息进行加密，确保消息传输安全
- **统一的认证授权**：集成 Spring Security，为 Actuator 端点提供认证保护

### 10.2 安全配置选项

#### 全局安全配置

```yaml
ms:
  middleware:
    security:
      enabled: true  # 是否启用安全功能
```

#### 缓存安全配置

```yaml
ms:
  middleware:
    security:
      cache:
        access-control-enabled: true  # 是否启用缓存访问控制
        key-prefix: "ms:cache:"  # 缓存键前缀
```

#### 锁安全配置

```yaml
ms:
  middleware:
    security:
      lock:
        access-control-enabled: true  # 是否启用锁访问控制
        key-prefix: "ms:lock:"  # 锁键前缀
```

#### 消息队列安全配置

```yaml
ms:
  middleware:
    security:
      mq:
        encryption-enabled: true  # 是否启用消息加密
        encryption-key: "your-secret-key"  # 消息加密密钥
```

### 10.3 安全工具类使用

```java
// 加密字符串
String encrypted = SecurityUtils.encryptAES("Hello, World!", "your-secret-key");

// 解密字符串
String decrypted = SecurityUtils.decryptAES(encrypted, "your-secret-key");

// 生成 MD5 哈希
String md5 = SecurityUtils.md5("test-data");

// 生成安全的缓存键
String secureCacheKey = SecurityUtils.generateSecureCacheKey("original-key", "ms:cache:");

// 生成安全的锁键
String secureLockKey = SecurityUtils.generateSecureLockKey("original-key", "ms:lock:");

// 检查权限
boolean hasPermission = SecurityUtils.checkPermission("READ", "READ,WRITE");
```

### 10.4 认证授权

本模块集成了 Spring Security，为 Actuator 端点提供认证保护。默认配置如下：

- **用户名**：admin
- **密码**：admin123
- **角色**：ADMIN

### 10.5 安全最佳实践

1. **使用强密钥**：为消息加密选择强密钥，避免使用弱密钥
2. **定期更换密钥**：定期更换加密密钥，提高安全性
3. **限制访问**：只授予必要的权限给用户
4. **监控异常**：监控安全相关的异常，及时发现潜在的安全问题
5. **使用 HTTPS**：在生产环境中使用 HTTPS 保护数据传输

## 11. 服务发现与注册

### 11.1 服务注册

```java
@Autowired
private ServiceDiscoveryAutoConfiguration.ServiceDiscoveryClient serviceDiscoveryClient;

// 注册服务实例
try {
    serviceDiscoveryClient.registerInstance("user-service", "192.168.1.100", 8080);
    System.out.println("服务注册成功");
} catch (Exception e) {
    System.err.println("服务注册失败: " + e.getMessage());
}

// 注册带权重的服务实例
try {
    serviceDiscoveryClient.registerInstance("user-service", "192.168.1.101", 8081, 1.5);
    System.out.println("带权重的服务注册成功");
} catch (Exception e) {
    System.err.println("服务注册失败: " + e.getMessage());
}
```

### 11.2 服务发现

```java
@Autowired
private ServiceDiscoveryAutoConfiguration.ServiceDiscoveryClient serviceDiscoveryClient;

// 获取服务实例列表
try {
    List<Instance> instances = serviceDiscoveryClient.getInstances("user-service");
    for (Instance instance : instances) {
        System.out.println("服务实例: " + instance.getIp() + ":" + instance.getPort());
    }
} catch (Exception e) {
    System.err.println("获取服务实例失败: " + e.getMessage());
}

// 获取健康的服务实例列表
try {
    List<Instance> healthyInstances = serviceDiscoveryClient.getHealthyInstances("user-service");
    for (Instance instance : healthyInstances) {
        System.out.println("健康服务实例: " + instance.getIp() + ":" + instance.getPort());
    }
} catch (Exception e) {
    System.err.println("获取健康服务实例失败: " + e.getMessage());
}

// 获取所有服务名称
try {
    List<String> services = serviceDiscoveryClient.getServices();
    System.out.println("所有服务: " + services);
} catch (Exception e) {
    System.err.println("获取服务列表失败: " + e.getMessage());
}
```

### 11.3 服务注销

```java
@Autowired
private ServiceDiscoveryAutoConfiguration.ServiceDiscoveryClient serviceDiscoveryClient;

// 注销服务实例
try {
    serviceDiscoveryClient.deregisterInstance("user-service", "192.168.1.100", 8080);
    System.out.println("服务注销成功");
} catch (Exception e) {
    System.err.println("服务注销失败: " + e.getMessage());
}
```
### 11.4 服务发现配置

```yaml
ms:
  middleware:
    discovery:
      enabled: true  # 是否启用服务发现功能
      server-addr: "localhost:8848"  # Nacos 服务器地址
      namespace: ""  # Nacos 命名空间
      group: "DEFAULT_GROUP"  # 服务分组
      cluster-name: "DEFAULT"  # 集群名称
```

### 11.5 技术优势

- **自动注册**：服务启动时自动注册到 Nacos 服务器
- **动态发现**：实时发现集群中的服务实例
- **负载均衡**：集成 Spring Cloud LoadBalancer，支持服务调用的负载均衡
- **高可用性**：Nacos 支持集群部署，确保服务发现的高可用性
- **配置管理**：同时支持配置中心功能，实现配置的集中管理和动态更新

### 11.6 最佳实践

1. **服务命名规范**：使用有意义的服务名称，便于识别和管理
2. **健康检查**：实现服务的健康检查，确保注册的服务实例是可用的
3. **版本管理**：在服务名称中包含版本信息，如 `user-service-v1`
4. **环境隔离**：使用不同的命名空间或分组隔离不同环境的服务
5. **监控告警**：监控服务注册和发现的状态，及时发现异常

## 12. 配置中心

### 12.1 获取配置

```java
@Autowired
private ConfigCenterAutoConfiguration.ConfigCenterClient configCenterClient;

// 获取配置
try {
    String config = configCenterClient.getConfig("app-config");
    System.out.println("配置内容: " + config);
} catch (Exception e) {
    System.err.println("获取配置失败: " + e.getMessage());
}
```

### 12.2 发布配置

```java
@Autowired
private ConfigCenterAutoConfiguration.ConfigCenterClient configCenterClient;

// 发布配置
try {
    boolean success = configCenterClient.publishConfig("app-config", "key: value");
    if (success) {
        System.out.println("配置发布成功");
    }
} catch (Exception e) {
    System.err.println("发布配置失败: " + e.getMessage());
}
```

### 12.3 监听配置变更

```java
@Autowired
private ConfigCenterAutoConfiguration.ConfigCenterClient configCenterClient;

// 添加配置监听器
try {
    configCenterClient.addListener("app-config", configInfo -> {
        System.out.println("配置已更新: " + configInfo);
        // 在这里处理配置变更逻辑
    });
    System.out.println("配置监听器添加成功");
} catch (Exception e) {
    System.err.println("添加监听器失败: " + e.getMessage());
}
```

### 12.4 配置中心配置

```yaml
ms:
  middleware:
    config:
      enabled: true  # 是否启用配置中心功能
      server-addr: "localhost:8848"  # Nacos 服务器地址
      namespace: ""  # Nacos 命名空间
      group: "DEFAULT_GROUP"  # 配置分组
      file-extension: "yaml"  # 配置文件扩展名
      timeout: 5000  # 配置获取超时时间（毫秒）
      refresh-enabled: true  # 是否启用配置自动刷新
```

### 12.5 技术优势

- **动态配置更新**：支持配置的热更新，无需重启应用
- **配置版本管理**：支持配置版本管理和历史版本回滚
- **多环境支持**：通过命名空间隔离不同环境的配置
- **配置监听**：支持配置变更监听，实时响应配置变化
- **权限控制**：支持配置的权限管理，确保配置安全

### 12.6 最佳实践

1. **配置命名规范**：使用有意义的配置名称，便于识别和管理
2. **环境隔离**：使用不同的命名空间或分组隔离不同环境的配置
3. **敏感信息加密**：对敏感配置信息进行加密存储
4. **配置版本管理**：定期备份配置，保留历史版本
5. **监控告警**：监控配置变更，及时发现异常
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
