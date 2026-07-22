package com.ms.middleware.console.agent.context;

import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.run.AutonomyRun;

import java.util.List;
import java.util.Optional;

/**
 * L2 降级检索（Phase 5.3 就有，5.4 仍保留作 fallback）。
 *
 * <p>做法：用用户问题里的关键词，调 Insight 扫「相似历史 run」列表——
 * <strong>不是</strong>语义向量，只是字符串/规则匹配，能力弱但零依赖（不需要 PG / embedding）。
 *
 * <p>由 {@link RetrievalAutoConfiguration} 注册 Bean，禁止再加 {@code @Component}，
 * 以免与 Composite 同时变成两个 {@link RetrievalContextProvider}。
 */
public class KeywordFallbackRetrievalProvider implements RetrievalContextProvider {

    private final MiddlewareInsightService insightService;

    public KeywordFallbackRetrievalProvider(MiddlewareInsightService insightService) {
        this.insightService = insightService;
    }

    @Override
    public Optional<String> retrieve(RetrievalQuery query, int budgetChars) {
        if (query == null || query.query() == null || query.query().isBlank()) {
            return Optional.empty();
        }
        int limit = 5;
        List<AutonomyRun> runs = insightService.searchSimilarRuns(query.query().trim(), limit);
        if (runs.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("（来源：").append(sourceLabel()).append("）\n");
        for (AutonomyRun run : runs) {
            sb.append("- runId=").append(run.getRunId())
                    .append(" status=").append(run.getStatus())
                    .append(" incident=")
                    .append(run.getPlan() != null ? run.getPlan().getIncidentType() : "—")
                    .append("\n");
        }
        return Optional.of(truncate(sb.toString().trim(), Math.max(128, budgetChars)));
    }

    @Override
    public String sourceLabel() {
        return "KEYWORD_FALLBACK";
    }

    private static String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }
}
