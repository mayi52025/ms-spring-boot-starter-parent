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

- [x] Micrometer：`ms.autonomy.run.total`、`ms.autonomy.mttr`

- [x] 控制台 STABLE 事件展示 MTTR

**Redisson 账本配置：**

```yaml
ms.middleware.autonomy.ledger:
  type: redisson
  max-runs: 200
  key-prefix: ms:autonomy:run
  ttl-hours: 168
```



### Phase 3 — MQ 场景 + 采纳推荐（下一步）



- [ ] EasyRules 实现 `AutonomyDecisionEngine`

- [ ] 规则：`MQ_DEGRADED` → 限流/延迟重试执行器

- [ ] `POST /api/recommendations/{id}/accept`

- [ ] 时间线区分 AUTO / ADVISE



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

com.ms.middleware.console/      # AI 控制台 API + UI 静态资源

```



`ai/HotKey*` 仍为统计信号，与 `autonomy` 并列，不合并包名。

