package com.ms.middleware.console.agent.grounding;

/**
 * LLM Tool Grounding 模式：relaxed 仅 Prompt 约束；strict 运维类问题强制预调 Insight Tool。
 */
public enum GroundingMode {

    RELAXED,
    STRICT;

    public static GroundingMode fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return RELAXED;
        }
        return "strict".equalsIgnoreCase(value.trim()) ? STRICT : RELAXED;
    }
}
