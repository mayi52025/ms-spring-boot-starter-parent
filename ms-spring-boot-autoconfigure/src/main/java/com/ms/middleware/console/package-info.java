/**
 * AI 控制台（Phase 1：REST + SSE + 静态页，无 LLM）。
 *
 * <h2>入口</h2>
 * <ul>
 *   <li>页面：{@code GET /ms-console} → static/ms-console/index.html</li>
 *   <li>API：{@code /ms-console/api/*} — 问题列表、run 详情、指标、聊天</li>
 *   <li>SSE：{@code GET /ms-console/api/stream} — 订阅账本时间线</li>
 * </ul>
 *
 * <p>控制台不直接访问 {@link com.ms.middleware.autonomy.run.AutonomyLedger}，
 * 统一通过 {@link com.ms.middleware.autonomy.insight.MiddlewareInsightService} 读数据，
 * 便于 Phase 5 把同一接口暴露为 LangChain4j Tool。</p>
 *
 * <p>配置开关：{@code ms.middleware.console.enabled=true}</p>
 */
package com.ms.middleware.console;
