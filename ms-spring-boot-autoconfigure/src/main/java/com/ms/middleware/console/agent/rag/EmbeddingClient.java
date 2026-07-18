package com.ms.middleware.console.agent.rag;

/**
 * 文本向量化；实现可为通义/OpenAI 兼容 HTTP，单测可 mock。
 */
public interface EmbeddingClient {

    /**
     * @return 稠密向量；长度应与配置 dimensions 一致
     */
    float[] embed(String text);
}
