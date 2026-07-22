package com.ms.middleware.console.agent.context;

import java.util.Optional;

/**
 * L2「长期记忆」检索 SPI。
 *
 * <p>谁在用：{@link ContextAssembler} 在用户问「上次 / 类似 / 文档」等意图时调用，
 * 把检索摘要塞进工作上下文，再交给 LLM。
 *
 * <p>实现演进：
 * <ul>
 *   <li>5.3：{@link KeywordFallbackRetrievalProvider}（关键词扫 Ledger）</li>
 *   <li>5.4：默认仍 Keyword；{@code rag.enabled=true} 时换成
 *       {@link CompositeRetrievalContextProvider}（向量优先，Keyword 兜底）</li>
 * </ul>
 *
 * <p>{@link #sourceLabel()} 必须反映<strong>当次</strong>实际命中来源（PGVECTOR / KEYWORD_FALLBACK），
 * 供 UI hint / 审计，禁止写死成「永远 PGVECTOR」。
 */
public interface RetrievalContextProvider {

    /**
     * @param query       检索请求（问题原文 + DOCUMENT/HISTORICAL_RUN 偏置）
     * @param budgetChars 字符预算上限（Assembler 按剩余预算传入）
     * @return 检索摘要文本；无命中返回 empty（上层可不注入这一层）
     */
    Optional<String> retrieve(RetrievalQuery query, int budgetChars);

    /** 供 UI / 审计标记检索来源；Composite 实现会跟当次实际链路变化 */
    String sourceLabel();
}
