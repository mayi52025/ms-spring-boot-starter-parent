package com.ms.middleware.console.agent.grounding;

import org.springframework.stereotype.Component;

/**
 * strict 模式：运维类问题预调 Insight Tool，将证据注入 LLM 上下文。
 */
@Component
public class StrictGroundingExecutor {

    private final GroundingPolicy policy;

    public StrictGroundingExecutor(GroundingPolicy policy) {
        this.policy = policy;
    }

    public record PreparedContext(String userMessage, String prefetchedEvidence) {
    }

    /**
     * 基于已编排的用户消息（含 5.3 工作上下文）追加 Tool 证据。
     */
    public PreparedContext prepareWithComposedMessage(
            String composedUserMessage,
            GroundingResolution resolution,
            GroundingMode mode,
            InsightToolGateway gateway) {

        if (mode != GroundingMode.STRICT || !resolution.opsQuestion()) {
            return new PreparedContext(composedUserMessage, "");
        }

        String evidence = gateway.executeRequiredTools(resolution);
        String augmented = composedUserMessage
                + "\n\n【系统已通过 Insight Tool 预查询，请基于以下事实回答，禁止编造】\n"
                + evidence;
        return new PreparedContext(augmented, evidence);
    }

    /** 兼容无 5.3 编排时的直接调用 */
    public PreparedContext prepare(
            String message,
            String runId,
            GroundingMode mode,
            InsightToolGateway gateway) {

        GroundingResolution resolution = policy.resolve(message, runId);
        String composed = buildBaseUserMessage(message, runId);
        return prepareWithComposedMessage(composed, resolution, mode, gateway);
    }

    public static String buildBaseUserMessage(String message, String runId) {
        if (runId != null && !runId.isBlank()) {
            return "【当前上下文 runId=" + runId.trim() + "】\n用户问题：" + message.trim();
        }
        return message != null ? message.trim() : "";
    }
}
