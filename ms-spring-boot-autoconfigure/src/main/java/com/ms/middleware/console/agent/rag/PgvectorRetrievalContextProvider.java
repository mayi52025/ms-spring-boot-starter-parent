package com.ms.middleware.console.agent.rag;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import com.ms.middleware.console.agent.context.RetrievalContextProvider;
import com.ms.middleware.console.agent.context.RetrievalQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Phase 5.4 向量检索：把用户问题 embed 后，到 PG+pgvector 做近邻查询。
 *
 * <p><b>定位：</b>只给 {@link com.ms.middleware.console.agent.context.CompositeRetrievalContextProvider}
 * 当「primary」用，不单独注册成对外 SPI，避免与 Keyword / Composite 抢 Bean。
 *
 * <p><b>质量门闩：</b>仅接受 {@code distance < maxDistance} 的命中；弱相关不注入，交给 Keyword。
 *
 * <p><b>tenant：</b>RUN 只查当前应用租户；DOC 还允许 {@code _global_}（classpath 内置手册）。
 */
public class PgvectorRetrievalContextProvider implements RetrievalContextProvider {

    /** 写入检索文本与 UI hint 的来源标记 */
    public static final String SOURCE = "PGVECTOR";

    private static final Logger log = LoggerFactory.getLogger(PgvectorRetrievalContextProvider.class);

    private final EmbeddingClient embeddingClient;
    private final RagVectorStore store;
    private final AutonomyTenantProvider tenantProvider;
    private final MsMiddlewareProperties.RagProperties ragProperties;

    public PgvectorRetrievalContextProvider(EmbeddingClient embeddingClient,
                                            RagVectorStore store,
                                            AutonomyTenantProvider tenantProvider,
                                            MsMiddlewareProperties.RagProperties ragProperties) {
        this.embeddingClient = embeddingClient;
        this.store = store;
        this.tenantProvider = tenantProvider;
        this.ragProperties = ragProperties;
    }

    @Override
    public Optional<String> retrieve(RetrievalQuery query, int budgetChars) {
        if (query == null || query.query() == null || query.query().isBlank()) {
            return Optional.empty();
        }
        float[] vector = embeddingClient.embed(query.query().trim());
        String tenant = tenantProvider != null ? tenantProvider.getTenant() : "";
        RagDocumentKind kindFilter = toKindFilter(query.kind());
        int topK = Math.max(1, ragProperties.getTopK());
        double maxDistance = ragProperties.getMaxDistance();
        List<RagSearchHit> hits = store.search(tenant, kindFilter, vector, topK, maxDistance);
        boolean docFallback = false;
        if (hits.isEmpty() && kindFilter == RagDocumentKind.RUN) {
            // 冷库无 RUN：允许 DOC 兜底，但用更严距离，避免「问历史却塞弱相关手册」
            double stricter = maxDistance > 0 ? Math.min(maxDistance, maxDistance * 0.85d) : 0.35d;
            hits = store.search(tenant, RagDocumentKind.DOC, vector, topK, stricter);
            docFallback = !hits.isEmpty();
        }
        if (hits.isEmpty()) {
            log.debug("pgvector no hit under maxDistance={} kind={}", maxDistance, kindFilter);
            return Optional.empty();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("（来源：").append(SOURCE);
        if (docFallback) {
            sb.append("，文档兜底");
        }
        sb.append("）\n");
        for (RagSearchHit hit : hits) {
            sb.append("- [").append(hit.kind()).append("] ref=").append(hit.refId());
            if (hit.chunkNo() > 0) {
                sb.append("#").append(hit.chunkNo());
            }
            if (hit.distance() >= 0) {
                sb.append(" dist=").append(String.format(java.util.Locale.ROOT, "%.3f", hit.distance()));
            }
            sb.append('\n');
            if (hit.content() != null && !hit.content().isBlank()) {
                sb.append(hit.content().trim()).append("\n\n");
            }
        }
        return Optional.of(truncate(sb.toString().trim(), Math.max(128, budgetChars)));
    }

    @Override
    public String sourceLabel() {
        return SOURCE;
    }

    private static RagDocumentKind toKindFilter(RetrievalQuery.RetrievalKind kind) {
        if (kind == RetrievalQuery.RetrievalKind.DOCUMENT) {
            return RagDocumentKind.DOC;
        }
        return RagDocumentKind.RUN;
    }

    private static String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }
}
