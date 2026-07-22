package com.ms.middleware.console.agent.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Phase 5.4 L2 对外唯一检索实现：先试向量库，不行再 Keyword。
 *
 * <h3>为什么需要它</h3>
 * 聊天侧（{@link ContextAssembler}）只依赖 {@link RetrievalContextProvider} 一个 SPI，
 * 不能同时注入两个 Provider 自己选——否则编排层会变复杂，且 UI 的「来源标签」容易撒谎。
 * 本类把「优先 pgvector / 降级 Keyword」封装在内部，对外仍是一个 Bean。
 *
 * <h3>降级策略</h3>
 * <ol>
 *   <li>primary（pgvector）有命中 → 用它，sourceLabel=PGVECTOR</li>
 *   <li>primary 返回 empty（库空、无相似、未过距离阈）→ Keyword</li>
 *   <li>primary 抛异常（embedding 挂、PG 挂）→ Keyword，并打 warn</li>
 * </ol>
 *
 * <h3>sourceLabel 为何用 ThreadLocal</h3>
 * {@code retrieve()} 与 {@code sourceLabel()} 是两次调用；
 * 若用普通字段记录「上次来源」，并发请求 A 命中向量、B 走 Keyword 时会互相覆盖，UI 标错来源。
 * ThreadLocal 保证「本线程当次 retrieve 实际用了谁」；每次 retrieve 开头 remove，避免线程池脏读。
 */
public class CompositeRetrievalContextProvider implements RetrievalContextProvider {

    private static final Logger log = LoggerFactory.getLogger(CompositeRetrievalContextProvider.class);

    private final RetrievalContextProvider primary;
    private final RetrievalContextProvider fallback;
    private final ThreadLocal<String> lastSource;

    public CompositeRetrievalContextProvider(RetrievalContextProvider primary,
                                             RetrievalContextProvider fallback) {
        this.primary = primary;
        this.fallback = fallback;
        this.lastSource = ThreadLocal.withInitial(fallback::sourceLabel);
    }

    @Override
    public Optional<String> retrieve(RetrievalQuery query, int budgetChars) {
        // 清空上次残留，避免线程池复用时脏 sourceLabel
        lastSource.remove();
        try {
            Optional<String> hit = primary.retrieve(query, budgetChars);
            if (hit.isPresent()) {
                lastSource.set(primary.sourceLabel());
                return hit;
            }
        } catch (Exception ex) {
            log.warn("pgvector retrieve failed, fallback to {}: {}",
                    fallback.sourceLabel(), ex.getMessage());
        }
        Optional<String> fallbackHit = fallback.retrieve(query, budgetChars);
        lastSource.set(fallback.sourceLabel());
        return fallbackHit;
    }

    @Override
    public String sourceLabel() {
        return lastSource.get();
    }
}
