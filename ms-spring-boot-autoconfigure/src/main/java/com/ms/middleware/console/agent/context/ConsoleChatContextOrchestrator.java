package com.ms.middleware.console.agent.context;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.console.agent.grounding.GroundingMode;
import com.ms.middleware.console.agent.grounding.GroundingResolution;
import com.ms.middleware.console.agent.grounding.StrictGroundingExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Phase 5.3 对话上下文编排：run 绑定、工作记忆、Assembler，与 5.2 Grounding 衔接。
 */
@Component
public class ConsoleChatContextOrchestrator {

    private final MsMiddlewareProperties properties;
    private final MiddlewareInsightService insightService;
    private final AgentOrchestrationPolicy orchestrationPolicy;
    private final ConversationStateStore stateStore;
    private final ContextAssembler contextAssembler;

    public ConsoleChatContextOrchestrator(MsMiddlewareProperties properties,
                                          MiddlewareInsightService insightService,
                                          AgentOrchestrationPolicy orchestrationPolicy,
                                          ConversationStateStore stateStore,
                                          ContextAssembler contextAssembler) {
        this.properties = properties;
        this.insightService = insightService;
        this.orchestrationPolicy = orchestrationPolicy;
        this.stateStore = stateStore;
        this.contextAssembler = contextAssembler;
    }

    /**
     * 编排结果：供 LLM / 规则模式构建最终用户消息，并在回复后更新会话态。
     */
    public record PreparedChatContext(
            String effectiveRunId,
            AgentOrchestrationDecision decision,
            GroundingResolution grounding,
            ConversationState conversationState,
            AssembledContext assembledContext,
            String messageForGrounding) {
    }

    public PreparedChatContext prepare(String message, String runId, String sessionId, GroundingMode groundingMode) {
        String effectiveRunId = resolveEffectiveRunId(runId);
        AgentOrchestrationDecision decision = orchestrationPolicy.resolve(message, effectiveRunId);
        ConversationState state = stateStore.getOrCreate(sessionId, effectiveRunId);
        state.setBoundRunId(effectiveRunId);

        // strict 且会 prefetch describeRun 时，L1 跳过 run 全文快照避免重复 token
        boolean skipRunSnapshot = groundingMode == GroundingMode.STRICT
                && decision.grounding().opsQuestion()
                && effectiveRunId != null
                && !effectiveRunId.isBlank();

        AssembledContext assembled = contextAssembler.assemble(
                decision,
                state,
                effectiveRunId,
                properties.getConsole().getContext(),
                skipRunSnapshot);

        String messageForGrounding = composeMessage(message, effectiveRunId, assembled);
        return new PreparedChatContext(
                effectiveRunId,
                decision,
                decision.grounding(),
                state,
                assembled,
                messageForGrounding);
    }

    /** 用户提问结束后更新压缩对话态（在 LLM 回复之后调用） */
    public void recordTurn(PreparedChatContext prepared,
                           String userMessage,
                           List<String> toolsUsed) {
        MsMiddlewareProperties.ContextProperties config = properties.getConsole().getContext();
        ConversationState state = prepared.conversationState();
        state.appendUserMessage(userMessage, config.getDialogUserMessages());
        state.setLastIntent(prepared.grounding().intent());
        state.setLastToolsUsed(toolsUsed);
    }

    private String resolveEffectiveRunId(String runId) {
        if (runId != null && !runId.isBlank()) {
            return runId.trim();
        }
        MsMiddlewareProperties.ContextProperties config = properties.getConsole().getContext();
        if (!config.isAutoBindSingleActiveRun()) {
            return null;
        }
        List<AutonomyRun> active = insightService.listActiveRuns();
        if (active.size() == 1) {
            return active.get(0).getRunId();
        }
        return null;
    }

    private static String composeMessage(String message, String runId, AssembledContext assembled) {
        StringBuilder sb = new StringBuilder();
        if (assembled != null && assembled.textBlock() != null && !assembled.textBlock().isBlank()) {
            sb.append(assembled.textBlock()).append("\n\n");
        }
        sb.append(StrictGroundingExecutor.buildBaseUserMessage(message, runId));
        return sb.toString();
    }
}
