# 安全功能使用说明

## 1. 安全功能概述

本模块提供了以下安全功能：

- **分布式缓存和锁的访问控制**：通过安全的键生成机制，防止未授权访问
- **消息队列的消息加密**：使用 AES 加密算法对消息进行加密，确保消息传输安全
- **统一的认证授权**：集成 Spring Security，为 Actuator 端点提供认证保护

## 2. 安全配置选项

### 2.1 全局安全配置

```yaml
ms:
  middleware:
    security:
      enabled: true  # 是否启用安全功能
```

### 2.2 缓存安全配置

```yaml
ms:
  middleware:
    security:
      cache:
        access-control-enabled: true  # 是否启用缓存访问控制
        key-prefix: "ms:cache:"  # 缓存键前缀
```

### 2.3 锁安全配置

```yaml
ms:
  middleware:
    security:
      lock:
        access-control-enabled: true  # 是否启用锁访问控制
        key-prefix: "ms:lock:"  # 锁键前缀
```

### 2.4 消息队列安全配置

```yaml
ms:
  middleware:
    security:
      mq:
        encryption-enabled: true  # 是否启用消息加密
        encryption-key: "your-secret-key"  # 消息加密密钥
```

## 3. 安全工具类使用

### 3.1 加密/解密

```java
// 加密字符串
String encrypted = SecurityUtils.encryptAES("Hello, World!", "your-secret-key");

// 解密字符串
String decrypted = SecurityUtils.decryptAES(encrypted, "your-secret-key");
```

### 3.2 生成安全的缓存键

```java
String secureCacheKey = SecurityUtils.generateSecureCacheKey("original-key", "ms:cache:");
```

### 3.3 生成安全的锁键

```java
String secureLockKey = SecurityUtils.generateSecureLockKey("original-key", "ms:lock:");
```

### 3.4 权限检查

```java
boolean hasPermission = SecurityUtils.checkPermission("READ", "READ,WRITE");
```

## 4. 认证授权

本模块集成了 Spring Security，为 Actuator 端点提供认证保护。默认配置如下：

- **用户名**：admin
- **密码**：admin123
- **角色**：ADMIN

如需修改默认配置，请在应用的安全配置类中覆盖相关配置。

## 5. 安全最佳实践

1. **使用强密钥**：为消息加密选择强密钥，避免使用弱密钥
2. **定期更换密钥**：定期更换加密密钥，提高安全性
3. **限制访问**：只授予必要的权限给用户
4. **监控异常**：监控安全相关的异常，及时发现潜在的安全问题
5. **使用 HTTPS**：在生产环境中使用 HTTPS 保护数据传输

## 6. 故障排除

### 6.1 消息解密失败

如果遇到消息解密失败的情况，请检查：
- 加密密钥是否正确
- 消息是否被篡改
- 加密/解密算法是否匹配

### 6.2 权限访问被拒绝

如果遇到权限访问被拒绝的情况，请检查：
- 用户是否拥有正确的权限
- 权限配置是否正确
- 安全配置是否启用

## 7. 示例配置

以下是一个完整的安全配置示例：

```yaml
ms:
  middleware:
    security:
      enabled: true
      cache:
        access-control-enabled: true
        key-prefix: "ms:cache:"
      lock:
        access-control-enabled: true
        key-prefix: "ms:lock:"
      mq:
        encryption-enabled: true
        encryption-key: "your-strong-secret-key"
```
