# 中间件自治与 AI 控制台 功能完成链路

## 需求背景
用户希望在 ms-spring-boot-starter 中加入「真正的智能中间件」能力：
- 故障时**先自动解决**（而非仅被动降级）
- 通过 **AI 控制台窗口** 实时呈现问题、时间线、推荐与聊天
- 部署到其他业务项目后，可通过 `/ms-console` 访问可视化运维界面

## 完成链路（Phase 1 MVP）

### 1. 架构设计
- 新增 `autonomy` 包：侦察（ContextBuilder）→ 计划（RuleEngine）→ 门控（Policy）→ 执行（Actuator）→ 账本（Ledger）→ 编排（Orchestrator）
- 新增 `console` 包：REST API + SSE 实时推送 + 静态 HTML 控制台页面
- 配置默认全关（`autonomy.enabled=false`），与现有功能零冲突

### 2. 核心实现
- **AutonomyContextBuilder**：聚合 Redis/Rabbit 健康、缓存命中率、MQ 失败计数、HotKey
- **AutonomyRuleEngine**：基于规则生成处置计划（目前支持 Redis 不可用场景）
- **AutonomyPolicy**：按 `auto-execute-max-risk` 决定 AUTO / ADVISE / DENY
- **AutonomyActuator**：调用现有 `FaultSelfHealing`、`HotKeyManager`、`MultiLevelCache` 降级逻辑
- **AutonomyLedger** + **SSE**：时间线事件实时推送到浏览器
- **静态控制台**：`static/ms-console/index.html`（问题列表 + 时间线 + 推荐 + 聊天输入框）

### 3. 配置与自动装配
- 在 `MsMiddlewareProperties` 新增 `AutonomyProperties` 与 `ConsoleProperties`
- `AutonomyAutoConfiguration` + `ConsoleAutoConfiguration` 通过 `@ConditionalOnProperty` 条件加载
- 更新 `spring.factories` 自动装配
- `application-ms-middleware.yml` 增加示例配置

### 4. 文档与测试
- 新增 `AUTONOMY_ROADMAP.md`（完整 Phase 1~6 路线图）
- 新增 `AutonomyRuleEngineTest` 单元测试
- README.md 新增「9. 中间件自治与 AI 控制台」章节

## 产出物
- 业务项目引入 starter 后，开启配置即可获得：
  - 后台自治闭环（Redis 故障自动 L1 降级 + 预热 + 自愈）
  - `/ms-console` 可视化窗口（实时时间线 + 推荐 + 规则聊天）
- 所有动作均可审计、可配置风险边界

## 下一步计划（已记录在 AUTONOMY_ROADMAP.md）
- Phase 2：Redisson 持久化账本 + 自治指标
- Phase 3：MQ 场景 + 推荐采纳按钮
- Phase 5：Spring AI 聊天接入

---
完成时间：2026-06-16
负责人：AI 辅助开发
