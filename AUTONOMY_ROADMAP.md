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

### Phase 3 — 场景识别 + 推荐采纳（已完成）



**目标：** 候选方案排序选优 + MQ 闭环 + 人机采纳；LLM 仍放 Phase 5。

#### 分步计划

| 步骤 | 内容 | 状态 |
|------|------|------|
| Step 0 | 决策契约与模型字段 | ✅ 已完成 |
| Step 1 | MQ/检测阈值统一 | ✅ 已完成 |
| Step 2 | 候选动作 + 规则选优 | ✅ 已完成 |
| Step 3 | MQ/Rabbit 执行器 | ✅ 已完成 |
| Step 4 | 推荐采纳 + 人机审计 | ✅ 已完成 |
| Step 5 | YAML 规则外置 | ✅ 已完成 |
| Step 6 | 指标 + Tool SPI + 文档 | ✅ 已完成 |
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

#### Step 4 推荐采纳 + 人机审计（已完成）

- `HumanAdoptionService`：配置推荐采纳/拒绝（幂等）；备选 ADVISE 动作人工采纳后走 Actuator
- `RecommendationAdoptionController`：`POST .../recommendations/{id}/accept|reject`、`POST .../runs/{runId}/actions/{rank}/accept`
- `AutonomyRecommendation` 增 `status/decidedAt/operator/rejectReason`；`PlannedAction.humanAccepted`
- 时间线 `ACCEPTED` phase 关联 `recommendationId`；控制台 `index.html` 采纳/拒绝/备选执行按钮
- 配置推荐仅审计，不自动改 Nacos（Phase 4 再接真改配置）
- 单测：`HumanAdoptionServiceTest`

#### Step 5 YAML 规则外置（已完成）

- `AutonomyRulesProperties`：`ms.middleware.autonomy.rules` 绑定 incident 识别链、Runbook、推荐
- `AutonomyRulesDefaults`：未配置时兜底，行为与 Step 2 硬编码等价
- `IncidentActionCatalog` / `AutonomyRuleEngine` 改为从 YAML 加载；`ActionSelector` 词典序选优不变
- 条件词典：`redis-unhealthy`、`mq-degraded`、`hot-keys-present` 等（无脚本引擎）
- 示例：`classpath:ms-middleware-autonomy-rules.example.yml`
- 单测：`IncidentActionCatalogTest`、`AutonomyRulesYamlTest`

#### Step 6 指标 + Tool SPI + 文档（已完成）

- **Micrometer 决策指标**（`AutonomyMetrics`）：
  - `ms.autonomy.action.auto.total`（tag: trigger=auto|human）
  - `ms.autonomy.recommendation.total` / `accepted.total` / `rejected.total`
  - `ms.autonomy.plan.confidence`（rank#1 证据强度）
- 埋点：`AutonomyOrchestrator`（PLAN/AUTO/RECOMMEND）、`HumanAdoptionService`（采纳/拒绝/人工执行）
- **Tool SPI**：`MiddlewareInsightTool` + `DefaultMiddlewareInsightTool`，控制台聊天与未来 LangChain4j 统一读数契约
- `ConsoleChatService` 改走 Tool；支持 trace messageId 查询
- 文档：`doc/autonomy/MQ自治演示剧本.md`
- 单测：`AutonomyMetricsTest`、`DefaultMiddlewareInsightToolTest`

#### Step 7 待办（可选）

- [ ] EasyRules 决策引擎（可选）

#### Phase 3.5 — 可观测与恢复性加固（已完成）

| 项 | 内容 |
|----|------|
| MQ 滑动窗口 | `MqFailureSlidingWindow` + `ms.mq.failed.recent`；STABLE 时清空窗口，支持恢复 |
| Runbook 审计 | `ms.middleware.autonomy.rules.version` 写入 PLAN 时间线 `runbook=` |
| 控制台两态 | 战时态 banner + 优化建议折叠；战后态 MTTR + 建议展开 |
| Demo 开关 | `demo.chaos.mq-fail.enabled` 替代 `demo.autonomy.mq-failure-consumer` |
| Grafana | `ms-autonomy.json` 增补 AUTO 率、采纳率、plan.confidence、窗口 MQ 失败 |
| Prometheus 告警 | `monitoring/prometheus/alerts/autonomy.yml` 示例规则 |
| 金路径测试 | `AutonomyGoldenPathTest`（MQ_DEGRADED → AUTO → STABLE） |

配置示例：

```yaml
ms.middleware.autonomy:
  mq-failure-window-minutes: 5   # 自治检测滑动窗口
  rules:
    version: prod-mq@2.1         # PLAN 审计
demo.chaos.mq-fail.enabled: false  # 故障注入演示，生产务必 false
```



### Phase 4 — 生产就绪：可验证结案 + 可操作排障 + 安全 + 多实例（进行中）

**目标：** 让用户一眼看懂「问题真的好了」；排障不依赖翻日志；控制台可上线；多实例不重复 AUTO。

**原则：** 编排层契约先行 → 控制台/API 可见 → 安全与分布式最后（避免返工）。

#### 分步计划

| 步骤 | 内容 | 状态 | 预估 |
|------|------|------|------|
| Step 0 | 恢复证据契约与模型字段 | ✅ 已完成 | 0.5d |
| Step 1 | STABLE 恢复依据（recoveryEvidence） | ✅ 已完成 | 1d |
| Step 2 | 失败 Trace 列表 API + 控制台 | ✅ 已完成 | 1d |
| Step 3 | 控制台鉴权（auth-token） | ✅ 已完成 | 1d |
| Step 4 | 采纳语义澄清 + chat 配置重命名 | ✅ 已完成 | 0.5d |
| Step 5 | 多实例分布式编排锁 | 待做 | 1～1.5d |
| Step 6 | Tenant 多应用隔离验收 | 待做 | 0.5d |
| Step 7 | 配置推荐 Nacos 草稿采纳（可选） | 待做 | 1～2d |

**启动方式：** 你说「启动 Step N」即从该步编码；Step 0 建议必做，Step 7 可跳过。

---

#### Step 0 — 恢复证据契约（✅ 已完成）

**解决什么：** Phase 3 只有 `STABLE + MTTR`，用户分不清「动作执行了」还是「指标真恢复了」。

**交付：**

| 类型 | 新增/扩展 | 说明 |
|------|-----------|------|
| `RecoveryEvidence` | 新模型 | incident 类型、恢复前后关键指标、判定条件摘要 |
| `AutonomyRun` | `recoveryEvidence`（可选） | STABLE 时写入，供 API/控制台展示 |
| `TimelineEvent` | STABLE message 结构化 | 兼容现有字符串，JSON 序列化可扩展 |
| `AutonomyTimelinePhase` | 不变 | 仍用 `STABLE`，不新增 phase |

**恢复证据字段（按 incident）：**

| incident | 对比字段 | 恢复条件（与 Step 3 一致） |
|----------|----------|---------------------------|
| `MQ_DEGRADED` | `mqFailedCount` 前→后 | 窗口内失败 < 阈值 |
| `REDIS_UNAVAILABLE` | `redisHealthy` false→true | Redis 探活恢复 |
| `RABBITMQ_UNAVAILABLE` | `rabbitMqHealthy` false→true | Rabbit 探活恢复 |
| `CACHE_DEGRADED` | `cacheHitRate` 前→后 | 命中率 ≥ 阈值 |

**单测：** `RecoveryEvidenceBuilderTest`（纯逻辑，无 Spring）。

**刻意不做：** 不改 STABLE 判定逻辑；不改编排 tick 频率。

---

#### Step 1 — STABLE 恢复依据落地（✅ 已完成）

**解决什么：** 控制台/API 展示「为什么判定已恢复」。

**交付：**

- `RecoveryEvidenceBuilder`：对比 run 开始时 context 快照 vs STABLE 时 context
- `AutonomyOrchestrator.stabilizeRun`：构建 evidence 写入 run + 丰富 STABLE 时间线  
  示例：`中间件指标恢复正常，MTTR=42s | MQ失败 5→0（阈值3）`
- 控制台 `index.html`：战后态 banner 下展示 recoveryEvidence 摘要
- `GET /ms-console/api/runs/{id}` 响应含 `recoveryEvidence`
- 文档：`MQ自治演示剧本.md` 增加「AUTO vs STABLE 验收」小节

**单测：** 扩展 `AutonomyGoldenPathTest` 断言 STABLE message 含证据。

---

#### Step 2 — 失败 Trace 可操作化（✅ 已完成）

**解决什么：** messageId 只在日志里，推荐「查 Trace」无法落地。

**交付：**

- `GET /ms-console/api/traces/failed?limit=20`：委托 `MessageTraceManager.listFailedTraces`
- 控制台：战时态下「最近失败消息」列表，一键复制 messageId / 跳转 Trace
- `ConsoleChatService` / Tool：支持「最近失败」「failed traces」关键词
- `MiddlewareInsightTool.listRecentFailedTraces(int)` SPI 扩展

**单测：** `DefaultMiddlewareInsightToolTest` + Controller 切片测试。

**刻意不做：** 不接 ES/持久化 Trace（仍内存，与 Phase 2 一致）。

---

#### Step 3 — 控制台鉴权（✅ 已完成）

**解决什么：** `/ms-console/**` 当前 permitAll，不能上生产。

**交付：**

- 配置 `ms.middleware.console.auth-token`（空则保持现状，兼容本地 Demo）
- `ConsoleAuthFilter` 或 Spring Security 专用链：校验 Header `X-MS-Console-Token` 或 `?token=`
- SSE `/api/stream` 同样受保护
- 静态页登录框（token 存 sessionStorage）或文档说明 curl 带 token
- `application-ms-middleware.yml` 示例

**单测：** MockMvc 无 token 401、正确 token 200。

**刻意不做：** 不接 OAuth/LDAP（Phase 4 只做 shared secret）。

---

#### Step 4 — 采纳语义澄清 + chat 配置重命名（✅ 已完成）

**解决什么：** `[ACCEPTED]` 被误解为「已改配置」；`chat-enabled=false` 反直觉。

**交付：**

- 控制台推荐区：`ACCEPTED` 显示 **「已记录采纳（未改配置）」** 徽章
- 备选动作人工执行：显示 **「人工触发」** 与 AUTO 区分（时间线已有，UI 强化）
- 配置重命名：`chat-enabled` → `llm-enabled`（旧 key 兼容一个版本 + deprecation 日志）
- EXECUTING 态提示：**「限流/自愈已启用，根因未消除时可能无法 STABLE」**（MQ 场景）
- 采纳时间线文案补充「未实际修改配置」

**单测：** `ConsolePropertiesTest`（llm-enabled / chat-enabled 绑定兼容）。

---

#### Step 5 — 多实例分布式编排锁（待做）

**解决什么：** 多 pod 同时 tick 可能重复 AUTO、争抢 activeRun。

**交付：**

- `AutonomyOrchestrator.tick` 入口：`Redisson` 分布式锁 `ms:autonomy:tick:{tenant}`（可配置 TTL）
- 配置 `ms.middleware.autonomy.orchestrator.distributed-lock-enabled`（默认 false，单机无感）
- 锁失败跳过本轮（不抛错，debug 日志）
- 与现有 JVM 内 `activeRunId` 共存：锁保 cluster，activeRunId 保 thread

**单测：** Redisson mock 或 `RedissonAutonomyLedgerTest` 同风格嵌入式测试。

**依赖：** Redisson 已存在（Phase 2）。

---

#### Step 6 — Tenant 多应用隔离验收（待做）

**解决什么：** Phase 1 预留 tenant，但未在多应用场景端到端验收。

**交付：**

- 确认 `AutonomyLedger` list/get 全链路 tenant 隔离（memory + redisson）
- `Middleware-demo`：可选第二应用或文档说明同 Redis 账本 key 前缀 + 不同 `spring.application.name`
- Insight API / 控制台 issues 仅当前 tenant
- 文档 + 单测：`InMemoryAutonomyLedgerTest` 增 cross-tenant 用例

**刻意不做：** 跨 tenant 运维大盘（留 Grafana label）。

---

#### Step 7 — 配置推荐 Nacos 草稿采纳（可选，待做）

**解决什么：** Phase 3 采纳仅审计；运维希望「点采纳 → 出配置 diff → 确认后生效」。

**交付：**

- `HumanAdoptionService.acceptRecommendation` 可选模式：`audit-only`（默认）| `nacos-draft`
- `NacosConfigDraftService`：生成 suggestedConfig 的 draft，不直接 publish（或 publish 到 draft dataId）
- 时间线 `ACCEPTED` 附带 draftId / diff 摘要
- 控制台：采纳后展示 diff + 「确认发布」二次按钮（Phase 4.5 或本步一并）

**依赖：** 业务已接 Nacos Config；demo 可 mock。

**刻意不做：** 自动 publish 生产配置（必须人工二次确认）。

---

#### Phase 4 配置预览（随 Step 逐步落地）

```yaml
ms:
  middleware:
    autonomy:
      orchestrator:
        distributed-lock-enabled: false   # Step 5
        tick-lock-ttl-seconds: 30
    console:
      auth-token: ""                      # Step 3，非空则启用鉴权
      llm-enabled: false                  # Step 4 重命名（兼容 chat-enabled）
```

#### Phase 4 与 Phase 3 的衔接

| Phase 3 遗留 | Phase 4 哪步解决 |
|--------------|------------------|
| AUTO 成功但用户不知是否恢复 | Step 0 + 1 recoveryEvidence |
| Trace 推荐无法操作 | Step 2 失败列表 API |
| 控制台不能上生产 | Step 3 auth-token |
| ACCEPTED 误解 | Step 4 语义 UI |
| 多实例重复 AUTO | Step 5 分布式锁 |
| tenant 未验收 | Step 6 |
| 采纳不改 Nacos | Step 7（可选） |

#### Phase 4 完成标准（Definition of Done）

- [x] MQ 演示：STABLE 时间线可见 `5→0` 类恢复依据
- [x] 控制台可查最近失败 messageId，不必翻 IDEA 日志
- [x] 配置 auth-token 后无 token 无法访问 API/SSE
- [ ] 两实例 order-system 仅一个实例执行 AUTO（Step 5 开启时）
- [ ] 自治相关单测全绿 + 金路径覆盖 recoveryEvidence

---



- [ ] optional LangChain4j 依赖

- [ ] Tool 基于 `MiddlewareInsightService`

- [ ] `llm-enabled=true` 时走 LLM（兼容旧 `chat-enabled`）



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

