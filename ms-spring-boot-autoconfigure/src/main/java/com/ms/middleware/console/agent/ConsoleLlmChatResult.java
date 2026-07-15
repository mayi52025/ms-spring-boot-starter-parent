package com.ms.middleware.console.agent;

import java.util.List;

/**
 * LLM 对话结果（含 Tool Grounding 审计）。
 */
public record ConsoleLlmChatResult(
        String reply,
        List<String> toolsUsed,
        boolean grounded) {

    public static ConsoleLlmChatResult of(String reply, List<String> toolsUsed, boolean grounded) {
        return new ConsoleLlmChatResult(reply, toolsUsed != null ? List.copyOf(toolsUsed) : List.of(), grounded);
    }
}
