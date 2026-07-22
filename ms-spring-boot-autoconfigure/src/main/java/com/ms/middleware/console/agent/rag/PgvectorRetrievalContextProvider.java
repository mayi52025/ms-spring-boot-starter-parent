package com.ms.middleware.console.agent.rag;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import com.ms.middleware.console.agent.context.RetrievalContextProvider;
import com.ms.middleware.console.agent.context.RetrievalQuery;

import java.util.List;
import java.util.Optional;

/**
 * Phase 5.4 向量检索：把用户问题 embed 后，到 PG+pgvector 做近邻查询。
 *
 * <p><b>定位：</b>只给 {@link com.ms.middleware.console.agent.context.CompositeRetrievalContextProvider}
 * 当「primary」用，不单独注册成对外 SPI，避免与 Keyword / Composite 抢 Bean。
 *
 * <p><b>和 Keyword 的差别：</b>
 * Keyword 是「字符串里有没有这几个字」扫 Ledger；
 * 这里是「语义相近」——问「上次 MQ 背压怎么止血」也能命中摘要里写 throttle 的 STABLE run。
 *
 * <p><b>tenant：</b>RUN 只查当前应用租户；DOC 还允许 {@code _global_}（classpath 内置手册）。
 */
public class PgvectorRetrievalContextProvider implements RetrievalContextProvider {

    /** 写入检索文本与 UI hint 的来源标记 */
    public static final String SOURCE = "PGVECTOR";

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
        // 1) 问题 → 向量（通义 embedding，与写入索引时同一模型/维度）
        float[] vector = embeddingClient.embed(query.query().trim());
        String tenant = tenantProvider != null ? tenantProvider.getTenant() : "";
        // 2) 按意图偏置：问「文档/规则」偏 DOC；问「上次/类似」偏 RUN
        RagDocumentKind kindFilter = toKindFilter(query.kind());
        int topK = Math.max(1, ragProperties.getTopK());
        List<RagSearchHit> hits = store.search(tenant, kindFilter, vector, topK);
        if (hits.isEmpty() && kindFilter == RagDocumentKind.RUN) {
            // 新环境往往还没有 STABLE 索引：再扫一遍内置文档，避免 L2 完全空
            hits = store.search(tenant, RagDocumentKind.DOC, vector, topK);
        }
        if (hits.isEmpty()) {
            return Optional.empty(); // Composite 会据此降级 Keyword
        }
        // 3) 拼成注入 LLM 的短摘要（受字符预算截断）
        StringBuilder sb = new StringBuilder();
        sb.append("（来源：").append(SOURCE).append("）\n");
        for (RagSearchHit hit : hits) {
            sb.append("- [").append(hit.kind()).append("] ref=").append(hit.refId());
            if (hit.chunkNo() > 0) {
                sb.append("#").append(hit.chunkNo());
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

    /** 编排层的 RetrievalKind → 表里的 kind 过滤 */
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
