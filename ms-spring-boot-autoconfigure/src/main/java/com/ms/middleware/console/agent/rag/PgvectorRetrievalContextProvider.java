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
 * <p><b>质量门闩：</b>仅接受 {@code distance < maxDistance} 的命中；弱相关不注入。
 * DOC 场景若向量未过阈，再用锚点词法补召回同一张手册表（仍标 {@link #SOURCE}）。
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
        String rawQuery = query.query().trim();
        // 短运维问句加领域 priming，拉近与手册 chunk 的余弦距离
        float[] vector = embeddingClient.embed(primingForEmbed(query.kind(), rawQuery));
        String tenant = tenantProvider != null ? tenantProvider.getTenant() : "";
        RagDocumentKind kindFilter = toKindFilter(query.kind());
        int topK = Math.max(1, ragProperties.getTopK());
        double maxDistance = ragProperties.getMaxDistance();
        List<RagSearchHit> hits = store.search(tenant, kindFilter, vector, topK, maxDistance);
        boolean docFallback = false;
        boolean lexical = false;
        if (hits.isEmpty() && kindFilter == RagDocumentKind.RUN) {
            // 冷库无 RUN：允许 DOC 兜底，但用更严距离，避免「问历史却塞弱相关手册」
            double stricter = maxDistance > 0 ? Math.min(maxDistance, maxDistance * 0.85d) : 0.35d;
            hits = store.search(tenant, RagDocumentKind.DOC, vector, topK, stricter);
            docFallback = !hits.isEmpty();
        }
        if (hits.isEmpty() && kindFilter == RagDocumentKind.DOC) {
            hits = store.searchLexical(tenant, RagDocumentKind.DOC, rawQuery, topK);
            lexical = !hits.isEmpty();
            if (lexical) {
                log.debug("pgvector lexical complement for query={}", rawQuery);
            }
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
        if (lexical) {
            sb.append("，词法补召回");
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
                // 单条再截一刀：命中块可能仍偏长，避免 topK 条把预算吃光
                int perHit = Math.max(64, ragProperties.getMaxCharsPerHit());
                sb.append(truncate(hit.content().trim(), perHit)).append("\n\n");
            }
        }
        return Optional.of(truncate(sb.toString().trim(), Math.max(128, budgetChars)));
    }

    @Override
    public String sourceLabel() {
        return SOURCE;
    }

    /**
     * 手册类问句加短 priming，改善口语短问与书面 chunk 的相似度。
     */
    static String primingForEmbed(RetrievalQuery.RetrievalKind kind, String query) {
        if (kind == RetrievalQuery.RetrievalKind.DOCUMENT) {
            return "中间件自治运维手册 分布式锁 限流止血 " + query;
        }
        return query;
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
