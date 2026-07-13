# MQ 自治演示剧本

> 适用项目：`middleware-demo/order-system`  
> 前置：starter 已 `mvn install`，Redis / RabbitMQ 可访问

## 1. 开启自治与控制台

在 `order-system` 的 `application.yml` 增加：

```yaml
ms:
  middleware:
    autonomy:
      enabled: true
      scan-interval-ms: 15000
      mq-failed-warn-threshold: 5
      auto-execute-min-confidence: 0.7
      mq:
        throttle-limit: 20
        throttle-window-seconds: 60
    console:
      enabled: true
      base-path: /ms-console
```

重启应用后访问：`http://localhost:8080/ms-console`

---

## 2. 场景 A：Redis 不可用（根因恢复）

### 触发

1. 停止 Redis（或改错 `ms.middleware.redis.host`）
2. 等待 1～2 个扫描周期（默认 15s）

### 预期时间线

| Phase | 含义 |
|-------|------|
| DETECT | 发现 Redis 不可用 |
| PLAN | 产出 Runbook 选优说明 |
| AUTO | 触发自愈 / L1 降级（rank#1 且证据足够） |
| RECOMMEND | 配置级推荐（TTL、热点预热等） |
| STABLE | Redis 恢复后结案 + MTTR |

### 控制台操作

- 左侧「当前问题」出现 run 卡片
- 「配置推荐」可点 **采纳 / 拒绝**
- 「备选方案」可对 ADVISE 动作 **采纳并执行**

### 恢复 Redis

观察 STABLE 与 MTTR；`/ms-console/api/metrics` 中 `lastMttrSeconds` 更新。

---

## 3. 场景 B：MQ 消费失败（限流 + 延迟重试）

> **为什么之前测不了？** order-system 原来**只发消息、没有消费者**，`mqFailedCount` 永远不会涨。
> 已在 demo 里加了 `MqFailureDemoConsumer`，开启 `demo.chaos.mq-fail.enabled=true` 即可。

### 触发步骤（按顺序做）

**① 确认 Redis / Rabbit 正常**（不要停 Redis，否则会变成场景 A）

**② 重装 starter 并重启 order-system**

```powershell
cd d:\projects\ms-spring-boot-starter-parent\ms-spring-boot-starter-parent
mvn clean install -DskipTests
# IDEA Reload Maven → Run order-system
```

启动日志应出现：`已注册 order-created 失败演示消费者`

**③ 连续创建 3 个订单**（每单 1 条消息 → 消费失败 1 次）

```powershell
1..3 | ForEach-Object {
  Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/orders `
    -ContentType "application/json" `
    -Body '{"customerId":"demo-user","amount":99.9}'
}
```

**④ 看失败计数**

```powershell
Invoke-RestMethod http://localhost:8080/ms-console/api/metrics
```

`mqFailedCount` 应 **≥ 3**（demo 已设 `mq-failed-warn-threshold: 3`；计数为**滑动窗口**内失败次数，非累计 Counter）。

**⑤ 等 10～20 秒**，再查：

```powershell
Invoke-RestMethod http://localhost:8080/ms-console/api/issues
```

应出现 `MQ_DEGRADED`；浏览器打开 `/ms-console` 看时间线。

**⑥ 测完关闭演示消费者**

`application.yml` → `demo.chaos.mq-fail.enabled: false`，重启。STABLE 时会清空窗口计数，可立即恢复。

### 预期行为

- incident 类型：`MQ_DEGRADED`
- rank#1：`THROTTLE_CONSUMER` **自动执行（AUTO）** — LOW 风险踩线即止血
- rank#2：`DELAYED_RETRY_BATCH` 仅备选（ADVISE），需人工「采纳并执行」
- 「优化建议」区：查 Trace 等配置建议，**不阻塞恢复**
- STABLE 时自动 `clearMqThrottle()`

### 聊天验证

控制台聊天输入：

- `指标` → 返回命中率、MQ 失败、MTTR、已完成自治数
- `trace <messageId>` → 查询失败消息追踪

---

## 4. Prometheus 指标验收（Step 6）

暴露端点（Spring Boot Actuator）后 scrape，关注：

| 指标 | 说明 |
|------|------|
| `ms_autonomy_run_total` | STABLE 结案计数 |
| `ms_autonomy_action_auto_total` | 自动/人工执行动作（tag: trigger=auto\|human） |
| `ms_autonomy_recommendation_total` | 配置推荐产出 |
| `ms_autonomy_recommendation_accepted_total` | 人工采纳 |
| `ms_autonomy_plan_confidence` | rank#1 证据强度分布 |

> Micrometer 导出时 `.` 可能变为 `_`，以实际 registry 为准。

---

## 5. 自定义 Runbook（Step 5）

复制 `ms-middleware-autonomy-rules.example.yml` 到业务 `application.yml` 的 `ms.middleware.autonomy.rules`，可调整 MQ 场景动作顺序与理由，**无需改 Java**。

---

## 6. 常见问题

| 现象 | 排查 |
|------|------|
| `/api/issues` 500 | 确认 ObjectMapper 已 `findAndRegisterModules()`（Instant 序列化） |
| 无 AUTO 只有 ADVISE | 证据强度低于 `auto-execute-min-confidence` |
| 控制台无 SSE | 检查 `console.enabled` 与浏览器 Network → EventSource |
| 指标为 0 | 需完整跑完 PLAN → STABLE 或人工采纳后才会累加 |

---

完成时间：Phase 3 Step 6  
关联文档：`AUTONOMY_ROADMAP.md`、`doc/autonomy/自治与AI控制台功能完成链路.md`
