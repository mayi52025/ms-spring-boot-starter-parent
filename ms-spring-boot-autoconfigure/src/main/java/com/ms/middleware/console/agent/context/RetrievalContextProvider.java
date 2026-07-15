package com.ms.middleware.console.agent.context;

import java.util.Optional;

/**
 * 长期记忆检索 SPI：5.3 提供 keyword 降级；5.4 接入 pgvector 向量检索。
 */
public interface RetrievalContextProvider {

    /**
     * @param query       检索请求
     * @param budgetChars 字符预算上限
     * @return 检索摘要文本；无命中返回 empty
     */
    Optional<String> retrieve(RetrievalQuery query, int budgetChars);

    /** 供 UI / 审计标记检索来源 */
    String sourceLabel();
}
