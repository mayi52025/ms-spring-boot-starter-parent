package com.ms.middleware.console.agent.context;

/**
 * 历史/文档检索请求（Phase 5.4 pgvector 实现；5.3 keyword 降级）。
 */
public record RetrievalQuery(String query, RetrievalKind kind) {

    public enum RetrievalKind {
        /** 历史 run 经验 */
        HISTORICAL_RUN,
        /** 文档 / 规则说明 */
        DOCUMENT
    }
}
