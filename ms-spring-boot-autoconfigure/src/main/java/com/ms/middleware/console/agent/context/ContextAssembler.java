package com.ms.middleware.console.agent.context;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.console.agent.grounding.InsightToolGateway;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 按优先级 + 字符预算装配工作上下文（L1），与 5.2 Tool prefetch 去重协作。
 */
@Component
public class ContextAssembler {

    private final MiddlewareInsightService insightService;
    private final RunSnapshotBuilder snapshotBuilder;
    private final RunContextCache runContextCache;
    private final RetrievalContextProvider retrievalProvider;

    public ContextAssembler(MiddlewareInsightService insightService,
                            RunSnapshotBuilder snapshotBuilder,
                            RunContextCache runContextCache,
                            RetrievalContextProvider retrievalProvider) {
        this.insightService = insightService;
        this.snapshotBuilder = snapshotBuilder;
        this.runContextCache = runContextCache;
        this.retrievalProvider = retrievalProvider;
    }

    /**
     * @param skipRunSnapshot 为 true 时跳过 RUN_SNAPSHOT（strict 已 prefetch describeRun）
     */
    public AssembledContext assemble(AgentOrchestrationDecision decision,
                                     ConversationState state,
                                     String boundRunId,
                                     MsMiddlewareProperties.ContextProperties config,
                                     boolean skipRunSnapshot) {
        if (!config.isEnabled()) {
            return AssembledContext.empty();
        }

        int budget = Math.max(256, config.getMaxChars());
        List<String> hints = new ArrayList<>();
        StringBuilder block = new StringBuilder();
        block.append("【工作上下文】\n");

        Optional<RunContextSnapshot> snapshot = Optional.empty();
        if (boundRunId != null && !boundRunId.isBlank()) {
            snapshot = runContextCache.get(
                    boundRunId,
                    config.getRunContextCacheSeconds(),
                    () -> snapshotBuilder.build(boundRunId, config.getTimelineEventLimit()));
        }

        for (ContextLayer layer : decision.contextLayers()) {
            if (budget <= 0) {
                break;
            }
            switch (layer) {
                case RUN_ANCHOR -> {
                    if (snapshot.isPresent()) {
                        String section = formatRunAnchor(snapshot.get());
                        budget = appendSection(block, hints, "runAnchor", section, budget);
                    }
                }
                case RUN_SNAPSHOT -> {
                    if (!skipRunSnapshot && snapshot.isPresent()) {
                        String section = formatRunSnapshot(snapshot.get());
                        budget = appendSection(block, hints, "runSnapshot", section, budget);
                    }
                }
                case WARTIME_SIGNAL -> {
                    if (snapshot.isPresent() && snapshot.get().wartime()) {
                        String section = "当前处于战时处置中，status=" + snapshot.get().status();
                        budget = appendSection(block, hints, "wartimeSignal", section, budget);
                    }
                }
                case FAILED_TRACES -> {
                    String section = loadFailedTraces(config.getWartimeTraceLimit());
                    if (!section.isBlank()) {
                        budget = appendSection(block, hints, "failedTraces×" + config.getWartimeTraceLimit(), section, budget);
                    }
                }
                case DIALOG_STATE -> {
                    String section = formatDialogState(state, config.getDialogUserMessages());
                    if (!section.isBlank()) {
                        budget = appendSection(block, hints, "dialogState", section, budget);
                    }
                }
                case RETRIEVAL -> {
                    if (decision.retrievalRequested()) {
                        RetrievalQuery query = AgentOrchestrationPolicy.toRetrievalQuery(decision);
                        Optional<String> retrieved = retrievalProvider.retrieve(query, Math.min(budget, 512));
                        if (retrieved.isPresent()) {
                            budget = appendSection(block, hints, retrievalProvider.sourceLabel(), retrieved.get(), budget);
                        }
                    }
                }
                default -> {
                    // no-op
                }
            }
        }

        if (hints.isEmpty()) {
            return AssembledContext.empty();
        }
        return new AssembledContext(block.toString().trim(), hints);
    }

    private String loadFailedTraces(int limit) {
        var traces = insightService.listFailedTraces(Math.max(1, limit));
        if (traces.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("最近失败 Trace：\n");
        for (var trace : traces) {
            sb.append("· messageId=").append(trace.getMessageId());
            if (trace.getErrorMessage() != null) {
                sb.append(" error=").append(trace.getErrorMessage());
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private static String formatRunAnchor(RunContextSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("绑定 run=").append(snapshot.runId())
                .append(" | status=").append(snapshot.status());
        if (snapshot.tenant() != null) {
            sb.append(" | tenant=").append(snapshot.tenant());
        }
        if (snapshot.incidentType() != null) {
            sb.append(" | incident=").append(snapshot.incidentType());
        }
        return sb.toString();
    }

    private static String formatRunSnapshot(RunContextSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        if (!snapshot.issues().isEmpty()) {
            sb.append("问题: ").append(snapshot.issues()).append("\n");
        }
        if (snapshot.mttrSeconds() != null) {
            sb.append("MTTR=").append(snapshot.mttrSeconds()).append("s\n");
        }
        if (!snapshot.timelineLines().isEmpty()) {
            sb.append("时间线(尾部):\n");
            for (String line : snapshot.timelineLines()) {
                sb.append(line).append("\n");
            }
        }
        if (snapshot.recoverySummary() != null) {
            sb.append("恢复证据: ").append(snapshot.recoverySummary()).append("\n");
        }
        return sb.toString().trim();
    }

    private static String formatDialogState(ConversationState state, int maxUserMessages) {
        if (state == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (state.getLastIntent() != null) {
            sb.append("上轮意图: ").append(state.getLastIntent()).append("\n");
        }
        if (!state.getLastToolsUsed().isEmpty()) {
            sb.append("上轮 Tool: ").append(String.join(", ", state.getLastToolsUsed())).append("\n");
        }
        List<String> messages = state.getRecentUserMessages();
        if (!messages.isEmpty()) {
            sb.append("最近用户原话(").append(Math.min(maxUserMessages, messages.size())).append("):\n");
            int from = Math.max(0, messages.size() - Math.max(1, maxUserMessages));
            for (int i = from; i < messages.size(); i++) {
                sb.append("· ").append(messages.get(i)).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /** 追加一段并扣减预算；返回剩余预算 */
    private static int appendSection(StringBuilder block,
                                     List<String> hints,
                                     String hint,
                                     String section,
                                     int budget) {
        if (section == null || section.isBlank()) {
            return budget;
        }
        String chunk = section.trim() + "\n\n";
        if (chunk.length() > budget) {
            chunk = chunk.substring(0, Math.max(0, budget)) + "...\n\n";
        }
        block.append(chunk);
        hints.add(hint);
        return budget - chunk.length();
    }
}
