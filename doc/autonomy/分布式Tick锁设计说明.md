# 分布式 Tick 锁设计说明（Step 5）

> 代码位置：`com.ms.middleware.autonomy.orchestrator.*`  
> 配置：`ms.middleware.autonomy.orchestrator.*`

---

## 1. 解决什么问题

order-system 在 K8s 里扩成 **2 个 Pod** 时，每个 Pod 都有自己的 `@Scheduled` 定时任务（`AutonomyScheduler`），默认每 10～30 秒触发一次 `AutonomyOrchestrator.tick()`。

**没有锁时会发生什么：**

```
Pod-A  tick → 检测到 MQ 失败 → PLAN → AUTO 限流
Pod-B  tick → 同时检测到 MQ 失败 → 又 PLAN → 又 AUTO 限流
```

后果：

- 同一故障周期可能 **重复执行** THROTTLE_CONSUMER
- 两个 Pod 各自维护 `activeRunId`，可能 **创建两个 run** 或争抢 Redisson 账本
- 控制台时间线、指标混乱

**目标：** 同一 tenant、同一扫描周期，**只有一个实例** 做编排决策和 AUTO。

---

## 2. 为什么用 Redisson 分布式锁

| 方案 | 不选 / 选用原因 |
|------|----------------|
| **Redisson RLock** ✅ | Phase 2 已接入 Redisson 做自治账本；复用 `RedissonConnectionManager` 与 Redis 自愈，不增加新组件 |
| DB 悲观锁 | 引入事务与表设计，与「中间件自治」场景过重 |
| ZooKeeper / etcd | 运维成本高，Demo 与中小团队不友好 |
| 仅 JVM `synchronized` | 只能锁单进程，多 Pod 无效 |

锁 key 与账本 key ** deliberately 分离**：

- 账本：`ms:autonomy:run:{tenant}:{runId}`
- Tick 锁：`ms:autonomy:tick:{tenant}`

避免把「互斥」和「业务数据」混在同一 key 上。

---

## 3. 架构：两层互斥

```
AutonomyScheduler（每个 Pod 都有）
        │
        ▼
  tick() 入口
        │
        ├─ AutonomyTickLock（集群层）── Redis ms:autonomy:tick:order-system
        │      获锁 → 继续
        │      未获锁 → debug 日志，本轮结束
        │
        ▼
  doTick()（业务层）
        │
        └─ activeRunId（JVM 层）── 本进程内复用 runId，避免重复 DETECT
```

- **TickLock**：多 Pod 之间选 leader  
- **activeRunId**：单 Pod 内多线程/多次 tick 之间复用 run（原有设计，Step 5 保留）

---

## 4. 核心 API 行为

### `AutonomyTickLock.runIfLeader(tenant, action)`

- **noop 实现**（默认）：直接 `action.run()`，与 Step 5 之前行为 100% 一致  
- **Redisson 实现**：
  - `tryLock(0, ttl, SECONDS)` — **不等待**，抢不到就跳过  
  - 持锁期间执行 `doTick()`  
  - `finally` 里 `unlock()`，正常路径立即释放  
  - `ttl` 防止 leader Pod 崩溃后锁死（租约过期后 follower 可接管）

### 配置

```yaml
ms:
  middleware:
    autonomy:
      orchestrator:
        distributed-lock-enabled: false   # 本地 Demo 保持 false
        tick-lock-ttl-seconds: 30
      ledger:
        type: redisson                    # 多实例建议 redisson，与锁共用 Redis
```

---

## 5. 多实例验证步骤

1. `distributed-lock-enabled: true`，`ledger.type: redisson`  
2. 启动两个 order-system（端口 8080 / 8081）  
3. 开启 `demo.chaos.mq-fail`，触发 MQ 故障  
4. 观察日志：
   - **一个** Pod：`Autonomy run ... EXECUTING`  
   - **另一个** Pod：`未获得 tick 分布式锁，跳过本轮`（debug 级别，需开 debug 日志）

---

## 6. 相关单测

| 测试类 | 验证点 |
|--------|--------|
| `RedissonAutonomyTickLockTest` | mock RLock：获锁执行 / 未获锁跳过 / unlock |
| `AutonomyOrchestratorDistributedLockTest` | 阻塞锁时 `contextBuilder.build()` 不被调用 |
| `AutonomyGoldenPathTest` | 默认 noop 构造器，金路径不受影响 |

---

## 7. 刻意不做

- 不做选主持久化（leader 身份不写 DB）；每轮 tick 抢锁即可  
- 锁失败不抛异常、不上报告警（避免 follower 正常跳过被当成故障）  
- 不替代账本里的 run 状态机；锁只包 tick 入口
