package com.ms.middleware.console.agent.rag;

/**
 * 向量检索命中的一行：kind + 引用 id + 分块序号 + 原文。
 * <p>供 {@link PgvectorRetrievalContextProvider} 拼成注入 LLM 的摘要文本。
 */
public record RagSearchHit(RagDocumentKind kind, String refId, int chunkNo, String content) {
}
