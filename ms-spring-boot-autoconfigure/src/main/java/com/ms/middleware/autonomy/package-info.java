/**
 * 中间件运行时自治引擎（Phase 1）。
 *
 * <h2>一次巡检的完整链路</h2>
 * <pre>
 * AutonomyScheduler.scan()
 *   └─ AutonomyOrchestrator.tick()
 *        ├─ AutonomyContextBuilder.build()     采集 Redis/MQ/指标/热点
 *        ├─ AutonomyDecisionEngine.plan()      生成处置计划（默认 AutonomyRuleEngine）
 *        ├─ AutonomyPolicy.evaluate()          按风险决定 AUTO 或 ADVISE
 *        ├─ AutonomyActuator.execute()         调用现有 FaultSelfHealing / HotKeyManager
 *        └─ AutonomyLedger                     记录 run + 时间线，并发布 SSE 事件
 * </pre>
 *
 * <h2>子包职责</h2>
 * <ul>
 *   <li>{@code context} — 一次扫描的中间件快照</li>
 *   <li>{@code decision} — 决策引擎接口（Phase 3 换 EasyRules）</li>
 *   <li>{@code plan} — 计划、动作、推荐</li>
 *   <li>{@code policy} — 自动执行风险门控</li>
 *   <li>{@code act} — 动作执行器，桥接现有中间件能力</li>
 *   <li>{@code run} — 账本与 run 生命周期</li>
 *   <li>{@code insight} — 控制台/LLM 统一读数门面</li>
 *   <li>{@code tenant} — 多应用隔离（默认 spring.application.name）</li>
 * </ul>
 *
 * <p>配置开关：{@code ms.middleware.autonomy.enabled=true}</p>
 *
 * <p><b>代码注释约定：</b>新增/修改的类与方法须附中文注释，说明职责、关键字段含义及与上下游模块的关系，
 * 便于他人阅读与维护。</p>
 */
package com.ms.middleware.autonomy;
