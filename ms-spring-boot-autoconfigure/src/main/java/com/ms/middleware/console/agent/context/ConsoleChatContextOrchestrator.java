package com.ms.middleware.console.agent.context;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.console.agent.grounding.GroundingMode;
import com.ms.middleware.console.agent.grounding.GroundingResolution;
import com.ms.middleware.console.agent.grounding.StrictGroundingExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

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
        // 显式 runId 优先；主页（runId 空）仅在「跟进/诊断」意图下才 auto-bind 单活跃故障
        String effectiveRunId = resolveEffectiveRunId(message, runId);
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

    /**
     * 解析有效 runId。
     * <ul>
     *   <li>请求已带 runId → 使用（用户明确选中历史/故障）</li>
     *   <li>主页全局意图（指标/活跃故障列表/闲聊）→ 不绑定，避免答成某个 EXECUTING</li>
     *   <li>跟进/诊断意图 + 仅 1 个活跃 run → 可选 auto-bind</li>
     * </ul>
     */
    String resolveEffectiveRunId(String message, String runId) {
        if (runId != null && !runId.isBlank()) {
            return runId.trim();
        }
        MsMiddlewareProperties.ContextProperties config = properties.getConsole().getContext();
        if (!config.isAutoBindSingleActiveRun()) {
            return null;
        }
        if (!shouldAutoBindOnEmptyRunId(message)) {
            return null;
        }
        List<AutonomyRun> active = insightService.listActiveRuns();
        if (active.size() == 1) {
            return active.get(0).getRunId();
        }
        return null;
    }

    /**
     * 未显式传 runId 时，仅对「跟进当前故障/诊断」类问题 auto-bind。
     * 主页问「当前有什么问题」「指标」「最近 run」保持全局查询。
     */
    static boolean shouldAutoBindOnEmptyRunId(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String n = message.toLowerCase(Locale.ROOT);
        // 全局盘点类：明确不绑定
        if (containsAny(n, "当前有什么问题", "有什么问题", "活跃故障", "指标", "metric", "命中率", "mttr",
                "最近 run", "最近失败", "你好", "hello")) {
            // 「为何还有问题」等跟进例外：含 为何/为什么/还没 时仍可绑定
            if (!containsAny(n, "为何", "为什么", "还没", "刚才", "这个 run", "这个run", "stable", "executing")) {
                return false;
            }
        }
        // 诊断 / 指代 / 跟进
        return containsAny(n, "为何", "为什么", "怎么回事", "原因", "还没", "刚才", "这个",
                "stable", "executing", "继续", "跟进", "为何还", "卡住");
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
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
