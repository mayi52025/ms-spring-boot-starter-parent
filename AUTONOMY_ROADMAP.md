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



### Phase 4 — 生产就绪：可验证结案 + 可操作排障 + 安全 + 多实例（✅ 已完成）

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
| Step 5 | 多实例分布式编排锁 | ✅ 已完成 | 1～1.5d |
| Step 6 | Tenant 多应用隔离验收 | ✅ 已完成 | 0.5d |
| Step 7 | 配置推荐 Nacos 草稿采纳（可选） | ✅ 已完成 | 1～2d |

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

#### Step 5 — 多实例分布式编排锁（✅ 已完成）

**解决什么：** 多 pod 同时 tick 可能重复 AUTO、争抢 activeRun。

**交付：**

- `AutonomyOrchestrator.tick` 入口：`RedissonAutonomyTickLock`，key `ms:autonomy:tick:{tenant}`（可配置 TTL）
- 配置 `ms.middleware.autonomy.orchestrator.distributed-lock-enabled`（默认 false，单机无感）
- 锁失败跳过本轮（不抛错，debug 日志）
- 与现有 JVM 内 `activeRunId` 共存：锁保 cluster，activeRunId 保 thread

**单测：** `RedissonAutonomyTickLockTest`、`AutonomyOrchestratorDistributedLockTest`。

**设计说明（中文）：** [`doc/autonomy/分布式Tick锁设计说明.md`](../doc/autonomy/分布式Tick锁设计说明.md)

---

#### Step 6 — Tenant 多应用隔离验收（✅ 已完成）

**解决什么：** Phase 1 预留 tenant，但未在多应用场景端到端验收。

**交付：**

- `AbstractAutonomyLedger.ensureTenant` 写入时强制绑定当前 tenant，防止 key 串写
- `AutonomyLedger` memory + redisson：`list/get` 按 tenant 隔离（单测 cross-tenant 验收）
- `DefaultMiddlewareInsightService` 列表/查询二次过滤，控制台 issues/history 仅当前 tenant
- 可选配置 `ms.middleware.autonomy.tenant-id` 覆盖 `spring.application.name`
- 单测：`InMemoryAutonomyLedgerTest`、`RedissonAutonomyLedgerTest`、`DefaultMiddlewareInsightServiceTenantTest`

**刻意不做：** 跨 tenant 运维大盘（留 Grafana label）。

---

#### Step 7 — 配置推荐 Nacos 草稿采纳（可选，✅ 已完成）

**解决什么：** Phase 3 采纳仅审计；运维希望「点采纳 → 出配置 diff → 确认后生效」。

**交付：**

- `HumanAdoptionService.acceptRecommendation` 可选模式：`audit-only`（默认）| `nacos-draft`
- `NacosConfigDraftService` + `InMemoryNacosConfigDraftService` / `NacosConfigDraftServiceImpl`：draft dataId，不直接 publish 生产
- `POST .../recommendations/{id}/publish` 二次确认发布；时间线 `ACCEPTED` + `PUBLISH`
- `AutonomyRecommendation`：`draftId` / `diffSummary` / `nacosPublished`；控制台 diff + 「确认发布到 Nacos」
- 单测：`HumanAdoptionServiceNacosDraftTest`、`InMemoryNacosConfigDraftServiceTest`
- Demo：`ms.middleware.autonomy.adoption.mode=nacos-draft`（内存模拟，无需真实 Nacos）

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
      adoption:
        mode: audit-only                  # Step 7：nacos-draft 时生成草稿 + 二次发布
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
| 采纳不改 Nacos | Step 7（nacos-draft 可选二次发布） |

#### Phase 4.5 — 生产加固（✅ 已完成）

**解决什么：** 双实例实测暴露 Redis 旧账本 JSON 反序列化失败；采纳审计缺 IP；tick 锁缺可观测指标。

**交付：**

| 项 | 内容 |
|----|------|
| 账本兼容 | `AutonomyRun.schemaVersion`；`AutonomyContext` 计算字段 `@JsonIgnore` + `ignoreUnknown` |
| 容错读 | `AutonomyRunSerde` 旧 JSON / 损坏 JSON → stub 降级，不拖垮 `/issues` |
| 指标 | `ms.autonomy.ledger.deserialize.errors.total`、`ms.autonomy.tick.leader/skipped.total` |
| 审计 | `TimelineEvent.operator/clientIp`；采纳/发布 API 自动注入客户端 IP |

**单测：** `AutonomyRunSerdeTest`（含 legacy `mqDegraded` JSON）

**刻意不做：** Trace 持久化、OAuth（留 Phase 5 或后续）

#### Phase 4 完成标准（Definition of Done）

- [x] MQ 演示：STABLE 时间线可见 `5→0` 类恢复依据
- [x] 控制台可查最近失败 messageId，不必翻 IDEA 日志
- [x] 配置 auth-token 后无 token 无法访问 API/SSE
- [x] 两实例 order-system 同 Redis 时 tick 互斥（`RedissonAutonomyTickLockDualInstanceIT` + 双实例 8080/8081 启分布式锁实测）
- [x] 自治相关单测全绿 + 金路径覆盖 recoveryEvidence

---

## 完整品定位（作品集）

**嵌入式中间件运维套件：** 进程内自治止血 + 账本证据 + 人机采纳 + React 控制台（IP 登录）+ 接地运维 Agent（Tool + RAG）。  
**目标：** 做完并验证、写入简历；不追求长期运营企业平台。  
**原则：** 自治环与 LLM 解耦；Agent 只读 Tool，写配置不绕过采纳/PUBLISH。

### 定稿技术栈

| 层 | 选型 | 不做 |
|----|------|------|
| 内核 | 已有 starter / 自治 / Redis 账本 / Rabbit | — |
| 控制台 | **React + Vite**，构建进 jar，`http://IP:port/ms-console` | 不再以静态 HTML 为主界面 |
| 鉴权 | `auth-token`（登录页） | 暂不上 OAuth |
| LLM | **OpenAI 兼容**（`base-url` + 可选 `api-key`）；可选 Ollama | 不绑死厂商；C 盘满可不装 Ollama |
| Agent | **LangChain4j · 单 Agent + 多 Tool** | Google ADK 多 Agent、A2A 实现 |
| Tool | 现有 `MiddlewareInsightTool` | LLM 不可直调采纳/发布 |
| RAG | **Embedding + PostgreSQL + pgvector** | Milvus、RagFlow 不进主仓 |
| 扩展（加分） | **MCP Server 只读** | 长期记忆、多 Agent 编排 |

---

### Phase 5 — 运维 Agent + React 控制台（作品集主阶段）

**目标：** 补齐「驾驶舱 + 认知层」，与流行 Agent 叙事对齐，且不削弱自治内核。

#### 建议顺序（按周弹性，简历向最短路径）

| 步 | 内容 | 交付 | 准备 |
|----|------|------|------|
| **5.0** | React 控制台替换静态页 ✅ | issues / 时间线 / 采纳 / SSE；token 登录；支持局域网 IP 访问 | `ms-console-ui`（Vite+React）；`npm run build` → `static/ms-console` |
| **5.1** | LLM 接入（OpenAI 兼容）✅ | `llm-enabled=true` 走 LangChain4j + Insight Tool；false 仍规则 | DeepSeek + `MS_LLM_API_KEY` |
| **5.2** | Tool Grounding ✅ | LLM 只调 Insight（run / issues / Trace / metrics）；禁写配置 | 单测：mock LLM 校验 tool 调用 |
| **5.3** | 短上下文 ✅ | 会话绑 `runId`；战时注入失败 Trace 摘要 | — |
| **5.4** | 轻量 RAG ✅ | STABLE / `classpath:rag/docs` → embedding → **pgvector**；Composite 降级 Keyword；硬化：距离门槛 / 意图同义词 / embedding 缓存 | Docker Postgres+pgvector；独立 embedding API（通义） |
| **5.5** | MCP 只读（加分）✅ | stdio 暴露 3 个只读 Tool（委托 `MiddlewareInsightTool`）；默认关 | Cursor `mcp.json` 样例见 README / `middleware-demo/mcp/` |

#### Phase 5 DoD（做完即可停、写简历）

- [x] React 控制台可经 IP + token 打开，功能不低于现静态页（源码 `ms-console-ui`，构建产物已进 jar）
- [x] `llm-enabled=true` 时自然语言可问：当前问题 / 指定 run / 失败 Trace / 为何 STABLE（有 Tool 证据）
- [x] 至少一次 RAG 问答命中历史或文档摘要（Composite：`PGVECTOR`；失败降级 `KEYWORD_FALLBACK`；演示见 `middleware-demo/rag/README.md`）
- [x] README：启动步骤、配置样例、演示脚本（主仓 README + `middleware-demo/rag/`；可选 Ollama 仍见既有说明）
- [x] （加分）MCP 只读可调通一个 Tool（`list_active_issues` / `describe_run` / `get_metrics_summary`；入口 `MsInsightMcpApplication`）

#### 刻意不做

- Google ADK / A2A / Milvus / RagFlow  
- LLM 自动 AUTO 或自动 publish Nacos  
- 跨应用统一运维大盘（仍「一应用一控制台」）

---

### Phase 6 — 演示收口与作品集包装（约 2～3 天）

**目标：** 可录屏、可给面试官复现，不是新功能大爆发。

| 项 | 内容 |
|----|------|
| Demo 剧本 | MQ 故障 → AUTO → STABLE 证据 → React 对话提问 →（可选）RAG 历史 |
| order-system | 默认配置对齐完整品；Compose 可选：Redis/Rabbit/Postgres |
| README / 架构图 | 完整品定位 + 技术栈 + 与「纯 Agent 课设」差异 |
| 录屏 | 5～8 分钟（简历附件） |
| 单测回归 | `autonomy.**` + Agent/Tool 烟雾测试 |

**Phase 6 DoD**

- [ ] 按 README 可从零起演示一遍  
- [ ] 简历项目描述与仓库一致（已完成 vs 可选加分写清）

---

### 开干前你需要准备的

| 类型 | 建议 |
|------|------|
| 必备 | JDK、Maven、Node 18+、现有 Redis/Rabbit（或 102 虚机） |
| 强烈建议 | 一个 OpenAI 兼容 API Key（豆包/通义/DeepSeek 等） |
| RAG | Docker 可跑 Postgres+pgvector（演示完可停） |
| 可选 | Ollama 装到 **D/E 盘** + `OLLAMA_MODELS`；C 盘满先跳过 |
| 不要先准备 | ADK、Milvus、RagFlow、企业 SSO |

---



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

com.ms.middleware.console/      # 控制台 API；React 构建产物静态托管
com.ms.middleware.console.agent/ # Phase 5：LangChain4j / Tool / context / rag（5.4 Composite 检索）
```

`ai/HotKey*` 仍为统计信号，与 `autonomy` 并列，不合并包名。

