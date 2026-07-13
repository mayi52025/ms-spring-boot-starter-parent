# 智能中间件：自治 + AI 控制台 — 分工路线图



## 产品形态（你的目标）



1. **自治引擎**：故障时先自动处置（缓存/MQ/自愈等组合）。

2. **AI 控制台**：问题列表 + 实时时间线 + 推荐 + 聊天。

3. **部署**：业务项目引入 `ms-spring-boot-starter`，打开配置即可。



## Phase 1 — MVP + 小幅重构（已完成）



| 模块 | 内容 |

|------|------|

| `autonomy` | 上下文聚合、规则计划、策略门控、执行器、编排、定时扫描 |

| `autonomy/run` | `AutonomyLedger` 接口 + `InMemoryAutonomyLedger`（默认） |

| `autonomy/decision` | `AutonomyDecisionEngine` 接口 + `AutonomyRuleEngine` |

| `autonomy/insight` | `MiddlewareInsightService` — 控制台 / 规则 / 未来 LangChain4j Tool 统一读数 |

| `autonomy/tenant` | `AutonomyTenantProvider` — 默认 `spring.application.name` |

| `console` | REST API、SSE 时间线、静态控制台页、规则聊天 |

| 配置 | `ms.middleware.autonomy.*`、`ms.middleware.console.*` |



### Phase 1 重构要点（为 Phase 2 铺路）



- 账本从具体类改为 **接口 + 内存实现**，配置 `ledger.type=memory`（默认）

- 决策引擎抽象为 **`AutonomyDecisionEngine`**，Phase 3 接 EasyRules 时不改编排层

- 控制台/chat **不再直连 Ledger**，统一走 **`MiddlewareInsightService`**

- Run 带 **tenant** 字段；稳定后可算 **MTTR**（`AutonomyRun.getMttrSeconds()`）



### 如何启用



```yaml

ms:

  middleware:

    autonomy:

      enabled: true

      scan-interval-ms: 30000

      auto-execute-max-risk: LOW

      ledger:

        type: memory    # Phase 2 可改为 redisson

        max-runs: 200

    console:

      enabled: true

      base-path: /ms-console

```



业务应用需引入 **`spring-boot-starter-web`**（控制台与 SSE）。



浏览器访问：`http://localhost:{port}/ms-console`



### API



| 方法 | 路径 | 说明 |

|------|------|------|

| GET | `/ms-console/api/issues` | 活跃故障列表 |

| GET | `/ms-console/api/runs` | 历史 run |

| GET | `/ms-console/api/runs/{id}` | 详情 + 时间线 |

| GET | `/ms-console/api/metrics` | 中间件指标快照 |

| GET | `/ms-console/api/stream` | SSE 实时事件 |

| POST | `/ms-console/api/chat` | 对话（当前为规则模式） |



---



## 建议完善点（相对你最初设想）



| 建议 | 原因 |

|------|------|

| 控制台生产必须鉴权 | 当前 Security 对 `/ms-console` 为 permitAll，上线前应对控制台单独加 token/角色 |

| 账本 MVP 用内存 | 重启丢失；Phase 2 迁 Redisson |

| 聊天先规则后 LLM | 避免没 API Key 时整个控制台不可用 |

| 推荐区「采纳」按钮 | Phase 3 接 Nacos 或返回配置片段 |



---



## 后续分工（Phase 2～6）



### Phase 2 — 持久化与指标（已完成）



- [x] `RedissonAutonomyLedger` + `ledger.type=redisson`

- [x] Micrometer：`ms_autonomy_run_total`、`ms_autonomy_mttr_seconds`（Prometheus 抓取 `/actuator/prometheus`）

- [x] 控制台 STABLE 事件展示 MTTR；`/ms-console/api/history` 展示已恢复 run

- [x] **P0 技术债（Redis 恢复统一）**：`RedissonConnectionManager` single-flight 重连；`DistributedCache` / `RedisRecoveryStrategy` / 账本共用；Lock / RateLimiter / IdempotentStore 每次 `ref.get()` 取最新 client；Redis 恢复后 `flushLocalFallbackToRedis` 回填账本

**Redisson 账本配置：**

```yaml
ms.middleware.autonomy.ledger:
  type: redisson
  max-runs: 200
  key-prefix: ms:autonomy:run
  ttl-hours: 168
```

**监控栈（`middleware-demo/monitoring`，部署在 102）：**

- Prometheus `:9090` 抓取 `order-system:8080/actuator/prometheus`（Windows 主机需 `server.address: 0.0.0.0` + 防火墙放行）
- Grafana `:3000` 预置 `ms-autonomy` 看板（run 总数、MTTR）

**Phase 2 已知限制：**

| 项 | 说明 |
|----|------|
| 编排锁 | 单 JVM 内 `recovering` 标志；多实例同时自愈需 Phase 4 分布式锁 |
| 账本降级 | Redis 不可用时写内存 `localFallback`，恢复后异步回填 |
| 控制台鉴权 | `/ms-console/**` 仍为 permitAll，生产需 Phase 4 token |
| Resilience4j 单测 | 依赖版本与 `PredicateCreator` 不兼容，与 P0 无关，待单独升级 |

### Phase 3 — 场景识别 + 推荐采纳（进行中）



**目标：** 候选方案排序选优 + MQ 闭环 + 人机采纳；LLM 仍放 Phase 5。

#### 分步计划

| 步骤 | 内容 | 状态 |
|------|------|------|
| Step 0 | 决策契约与模型字段 | ✅ 已完成 |
| Step 1 | MQ/检测阈值统一 | ✅ 已完成 |
| Step 2 | 候选动作 + 规则选优 | ✅ 已完成 |
| Step 3 | MQ/Rabbit 执行器 | ✅ 已完成 |
| Step 4 | 推荐采纳 + 人机审计 | 待做 |
| Step 5 | YAML 规则外置 | 待做 |
| Step 6 | 指标 + Tool SPI + 文档 | 待做 |
| Step 7 | EasyRules（可选） | 待做 |

#### Step 0 决策契约（已完成）

**决策流水线（Step 2 起逐步实现）：**

```
Context → 候选动作池（Runbook）→ ActionSelector 规则选优
       → EvidenceStrength 证据评估
       → Policy 门控（风险 + 证据强度）
       → AUTO 执行 rank#1 且 LOW 风险
       → RECOMMEND 展示其余方案与配置建议
       → [ACCEPTED] 人工采纳（Step 4）
       → STABLE
```

**模型字段：**

| 类型 | 新增字段 | 说明 |
|------|----------|------|
| `PlannedAction` | `rank`, `score`, `confidence` | 排序位(1-based)、保留字段(恒0)、证据强度；未选优前 rank 为 0 |
| `AutonomyRecommendation` | `recommendationId` | 8 位 UUID，采纳 API 主键 |
| `TimelineEvent` | `recommendationId`（可选） | ACCEPTED 事件关联推荐 |
| `AutonomyTimelinePhase` | 枚举 | DETECT / PLAN / AUTO / ADVISE / RECOMMEND / ACCEPTED / STABLE |

**时间线 phase 语义：**

- `DETECT` — 发现故障
- `PLAN` — 产出计划（Step 2 起含排序理由）
- `AUTO` — 已自动执行（兼容旧 `ACTION` 字符串）
- `ADVISE` — 仅建议，等人确认
- `RECOMMEND` — 配置级推荐
- `ACCEPTED` — 人工采纳（Step 4）
- `STABLE` — 结案 + MTTR

**Step 0 刻意未改：** 编排器仍写 `ACTION` phase；`AutonomyRuleEngine` 逻辑不变；无新执行器。

#### 阈值统一（已完成）

- `AutonomyContext` 携带 `mqFailedWarnThreshold`、`cacheHitRateWarnThreshold`
- `isMqDegraded()` / `isCacheDegraded()` 供 issues、规则引擎、STABLE 共用
- `AutonomyRuleEngine`：`MQ_DEGRADED` 仅在 `>= 阈值` 时触发（不再 `> 0`）
- 单测：`AutonomyContextBuilderTest`、`AutonomyRuleEngineTest` 边界用例

#### Step 2 规则选优（已完成）

- `IncidentActionCatalog`：各 incident 的 Runbook 候选（顺序、是否治根因）
- `ActionSelector`：词典序选优（根因 > 风险 > Runbook 顺序），**不用浮点加权打分**
- `EvidenceStrengthEvaluator`：独立评估证据强度，供 Policy 门控（踩线 ADVISE、明显超标 AUTO）
- `AutonomyPlan.rankingSummary`：PLAN 时间线输出可解释的选优依据
- 配置 `auto-execute-min-confidence`（默认 0.7）；编排器仅 rank#1 可 AUTO
- 单测：`ActionSelectorTest`、`EvidenceStrengthEvaluatorTest`、`AutonomyPolicyTest`

#### Step 3 MQ/Rabbit 执行器（已完成）

- `MqConsumerThrottle`：THROTTLE_CONSUMER 对接 `RedisRateLimiter`，消费路径背压
- `MqDelayedRetryExecutor`：DELAYED_RETRY_BATCH 从 trace 取失败消息 `sendDelayed` 重投
- `RabbitMessageQueue` 消费前 `awaitPermit()`；失败时 `storeRetryPayload` 供重试
- `AutonomyOrchestrator` STABLE 时自动 `clearMqThrottle()`
- 配置 `ms.middleware.autonomy.mq.*`（throttle-limit、delayed-retry-delay-ms 等）
- 单测：`MqConsumerThrottleTest`、`MqDelayedRetryExecutorTest`、`AutonomyActuatorMqTest`

#### Step 4～7 待办（原 Phase 3 项）

- [ ] `POST /api/recommendations/{id}/accept`

- [ ] 时间线统一使用 `AUTO`（替代 `ACTION`）



### Phase 4 — 安全与多应用（3 天）



- [ ] `ms.middleware.console.auth-token`

- [ ] 多实例 run 按 tenant 隔离（Phase 1 已预留）



### Phase 5 — LangChain4j 聊天（1～2 周）



- [ ] optional LangChain4j 依赖

- [ ] Tool 基于 `MiddlewareInsightService`

- [ ] `chat-enabled=true` 时走 LLM



### Phase 6 — middleware-demo 演示（2 天）



- [ ] order-system 开 autonomy + console

- [ ] README：停 Redis → 看窗口时间线



---



## 包结构



```

com.ms.middleware.autonomy/

  ├── decision/     # AutonomyDecisionEngine

  ├── insight/      # MiddlewareInsightService

  ├── run/          # AutonomyLedger + InMemory / Redisson 实现
  ├── metrics/      # AutonomyMetrics（Micrometer）
  └── tenant/       # AutonomyTenantProvider

com.ms.middleware.redis/        # RedissonConnectionManager、RedissonProbes（统一恢复）

com.ms.middleware.console/      # AI 控制台 API + UI 静态资源

```



`ai/HotKey*` 仍为统计信号，与 `autonomy` 并列，不合并包名。

