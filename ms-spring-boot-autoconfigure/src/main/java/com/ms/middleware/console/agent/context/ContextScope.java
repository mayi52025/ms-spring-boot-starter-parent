package com.ms.middleware.console.agent.context;

/**
 * 对话上下文作用域：有 runId 时以 incident 为中心，否则全局。
 */
public enum ContextScope {

    /** 绑定到具体自治 run */
    RUN,
    /** 未绑定 run 的全局问答 */
    GLOBAL
}
