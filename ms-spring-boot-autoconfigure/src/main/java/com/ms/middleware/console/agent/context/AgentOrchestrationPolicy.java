package com.ms.middleware.console.agent.context;

import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.console.agent.grounding.GroundingIntent;
import com.ms.middleware.console.agent.grounding.GroundingPolicy;
import com.ms.middleware.console.agent.grounding.GroundingResolution;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 统一 Agent 编排策略：在 {@link GroundingPolicy} 之上补充上下文分层与检索路由。
 */
@Component
public class AgentOrchestrationPolicy {

    private final GroundingPolicy groundingPolicy;
    private final MiddlewareInsightService insightService;

    public AgentOrchestrationPolicy(GroundingPolicy groundingPolicy,
                                    MiddlewareInsightService insightService) {
        this.groundingPolicy = groundingPolicy;
        this.insightService = insightService;
    }

    /**
     * 一次判定：Tool 路由 + 上下文分层 + 是否走长期检索。
     */
    public AgentOrchestrationDecision resolve(String message, String runId) {
        GroundingResolution grounding = groundingPolicy.resolve(message, runId);
        String normalized = message != null ? message.toLowerCase() : "";

        ContextScope scope = runId != null && !runId.isBlank() ? ContextScope.RUN : ContextScope.GLOBAL;
        Optional<AutonomyRun> boundRun = scope == ContextScope.RUN
                ? insightService.getRun(runId.trim())
                : Optional.empty();

        boolean retrievalRequested = isRetrievalRequested(normalized, grounding.intent());
        String retrievalQuery = retrievalRequested ? extractRetrievalQuery(message, normalized) : "";

        List<ContextLayer> layers = new ArrayList<>();
        if (scope == ContextScope.RUN) {
            layers.add(ContextLayer.RUN_ANCHOR);
            layers.add(ContextLayer.RUN_SNAPSHOT);
            if (isWartime(boundRun)) {
                if (shouldInjectFailedTraces(normalized, grounding.intent())) {
                    layers.add(ContextLayer.FAILED_TRACES);
                } else {
                    layers.add(ContextLayer.WARTIME_SIGNAL);
                }
            }
        }
        layers.add(ContextLayer.DIALOG_STATE);
        if (retrievalRequested) {
            layers.add(ContextLayer.RETRIEVAL);
        }

        return new AgentOrchestrationDecision(grounding, scope, layers, retrievalRequested, retrievalQuery);
    }

    /** 是否处于战时（故障处置中） */
    static boolean isWartime(Optional<AutonomyRun> run) {
        if (run.isEmpty()) {
            return false;
        }
        AutonomyRunStatus status = run.get().getStatus();
        return status == AutonomyRunStatus.DETECTED
                || status == AutonomyRunStatus.PLANNED
                || status == AutonomyRunStatus.EXECUTING;
    }

    /** 诊断类意图才注入失败 Trace，避免战时问「指标」被 Trace 污染 */
    static boolean shouldInjectFailedTraces(String normalized, GroundingIntent intent) {
        if (intent == GroundingIntent.RECENT_FAILED_TRACES
                || intent == GroundingIntent.TRACE_LOOKUP
                || intent == GroundingIntent.RUN_DETAIL) {
            return true;
        }
        return containsAny(normalized, "为什么", "为何", "原因", "失败", "trace", "stable", "还没", "messageid");
    }

    /**
     * 是否走 L2 检索。同义词刻意偏运维场景，避免必须说「文档」才命中手册。
     */
    static boolean isRetrievalRequested(String normalized, GroundingIntent intent) {
        if (intent == GroundingIntent.SIMILAR_RUNS) {
            return true;
        }
        return containsAny(normalized,
                "上次", "历史", "以前", "曾经", "类似", "相似",
                "文档", "手册", "playbook", "runbook", "roadmap", "规则",
                "tick", "限流", "止血", "背压", "throttle", "mttr", "结案");
    }

    static String extractRetrievalQuery(String message, String normalized) {
        if (message == null || message.isBlank()) {
            return "MQ";
        }
        return message.trim();
    }

    static RetrievalQuery toRetrievalQuery(AgentOrchestrationDecision decision) {
        String normalized = decision.retrievalQuery() != null ? decision.retrievalQuery().toLowerCase() : "";
        RetrievalQuery.RetrievalKind kind = containsAny(normalized,
                "文档", "手册", "playbook", "runbook", "roadmap", "规则", "tick")
                ? RetrievalQuery.RetrievalKind.DOCUMENT
                : RetrievalQuery.RetrievalKind.HISTORICAL_RUN;
        return new RetrievalQuery(decision.retrievalQuery(), kind);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
