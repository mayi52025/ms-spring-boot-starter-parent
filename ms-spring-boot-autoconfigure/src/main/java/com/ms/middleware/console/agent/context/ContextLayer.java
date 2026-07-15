package com.ms.middleware.console.agent.context;

/**
 * 工作上下文分层（按优先级从高到低装配，受字符预算约束）。
 */
public enum ContextLayer {

    /** run 锚点：runId / status / tenant */
    RUN_ANCHOR,
    /** run 快照：timeline 尾部、recovery 证据（不含 Tool 已 prefetch 的全文） */
    RUN_SNAPSHOT,
    /** 战时一行信号（非诊断意图时不灌 Trace 列表） */
    WARTIME_SIGNAL,
    /** 最近失败 Trace 摘要（intent-gated） */
    FAILED_TRACES,
    /** 压缩对话态：最近用户原话 + 上轮 Tool */
    DIALOG_STATE,
    /** 历史/文档检索（5.4 向量；5.3 keyword 降级） */
    RETRIEVAL
}
