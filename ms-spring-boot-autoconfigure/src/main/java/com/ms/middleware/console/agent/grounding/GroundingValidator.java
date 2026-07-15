package com.ms.middleware.console.agent.grounding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * 校验 LLM 回复是否有 Tool 证据支撑（strict 模式）。
 */
@Component
public class GroundingValidator {

    public record ValidationResult(boolean grounded, String fallbackReply) {

        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fallback(String reply) {
            return new ValidationResult(false, reply);
        }
    }

    public ValidationResult validate(
            String llmReply,
            List<String> toolsUsed,
            GroundingResolution resolution,
            GroundingMode mode,
            String prefetchedEvidence) {

        if (mode != GroundingMode.STRICT || !resolution.opsQuestion()) {
            return ValidationResult.ok();
        }

        if (toolsUsed == null || toolsUsed.isEmpty()) {
            if (prefetchedEvidence != null && !prefetchedEvidence.isBlank()) {
                return ValidationResult.fallback(buildEvidenceReply(prefetchedEvidence));
            }
            return ValidationResult.fallback("（Grounding）未获取到 Insight Tool 证据，无法回答该运维问题。");
        }

        Set<String> used = new HashSet<>(toolsUsed);
        for (InsightToolInvocation required : resolution.requiredTools()) {
            if (!used.contains(required.tool().langChainName())) {
                if (prefetchedEvidence != null && !prefetchedEvidence.isBlank()) {
                    return ValidationResult.fallback(buildEvidenceReply(prefetchedEvidence));
                }
                return ValidationResult.fallback("（Grounding）缺少必调 Tool：" + required.tool().langChainName());
            }
        }

        if (llmReply == null || llmReply.isBlank()) {
            if (prefetchedEvidence != null && !prefetchedEvidence.isBlank()) {
                return ValidationResult.fallback(buildEvidenceReply(prefetchedEvidence));
            }
            return ValidationResult.fallback("（Grounding）LLM 未返回有效内容。");
        }

        return ValidationResult.ok();
    }

    private static String buildEvidenceReply(String evidence) {
        return "（Grounding）基于 Insight Tool 数据：\n\n" + evidence.trim();
    }
}
