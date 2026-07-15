package com.ms.middleware.console.agent.context;

import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.run.AutonomyRun;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 5.3 降级检索：关键词过滤相似 run，非向量 RAG（5.4 替换为 pgvector 实现）。
 */
@Component
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
