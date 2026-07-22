package com.ms.middleware.console.agent.rag;

/**
 * 向量检索命中行。
 *
 * @param distance pgvector {@code <=>} 余弦距离（越小越相似）；未计算时可为负数占位
 */
public record RagSearchHit(RagDocumentKind kind, String refId, int chunkNo, String content, double distance) {

    public RagSearchHit(RagDocumentKind kind, String refId, int chunkNo, String content) {
        this(kind, refId, chunkNo, content, -1d);
    }
}
