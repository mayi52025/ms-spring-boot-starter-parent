package com.ms.middleware.console.agent;

import java.util.List;

/**
 * LLM 对话结果（含 Tool Grounding 与工作上下文审计）。
 */
public record ConsoleLlmChatResult(
        String reply,
        List<String> toolsUsed,
        boolean grounded,
        List<String> contextHints,
        String boundRunId) {

    public static ConsoleLlmChatResult of(String reply, List<String> toolsUsed, boolean grounded) {
        return of(reply, toolsUsed, grounded, List.of(), null);
    }

    public static ConsoleLlmChatResult of(
            String reply,
            List<String> toolsUsed,
            boolean grounded,
            List<String> contextHints,
            String boundRunId) {
        return new ConsoleLlmChatResult(
                reply,
                toolsUsed != null ? List.copyOf(toolsUsed) : List.of(),
                grounded,
                contextHints != null ? List.copyOf(contextHints) : List.of(),
                boundRunId);
    }
}
