package com.ms.middleware.console.agent.context;

import java.util.List;

/**
 * 装配后的工作上下文块（注入 LLM 用户消息 + UI hints）。
 */
public record AssembledContext(String textBlock, List<String> contextHints) {

    public AssembledContext {
        contextHints = contextHints != null ? List.copyOf(contextHints) : List.of();
    }

    public static AssembledContext empty() {
        return new AssembledContext("", List.of());
    }
}
