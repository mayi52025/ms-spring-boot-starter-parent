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
 *   <li>primary 返回 empty（库空、无相似）→ Keyword</li>
 *   <li>primary 抛异常（embedding 挂、PG 挂）→ Keyword，并打 warn</li>
 * </ol>
 *
 * <h3>sourceLabel 为何用 ThreadLocal</h3>
 * {@code retrieve()} 与 {@code sourceLabel()} 是两次调用；
 * 若用普通字段记录「上次来源」，并发请求 A 命中向量、B 走 Keyword 时会互相覆盖，UI 标错来源。
 * ThreadLocal 保证「本线程当次 retrieve 实际用了谁」。
 */
public class CompositeRetrievalContextProvider implements RetrievalContextProvider {

    private static final Logger log = LoggerFactory.getLogger(CompositeRetrievalContextProvider.class);

    /** 优先：向量检索（pgvector） */
    private final RetrievalContextProvider primary;
    /** 兜底：关键词扫 Ledger 相似 run */
    private final RetrievalContextProvider fallback;
    /** 本线程最近一次 retrieve 实际命中的来源标签 */
    private final ThreadLocal<String> lastSource;

    public CompositeRetrievalContextProvider(RetrievalContextProvider primary,
                                             RetrievalContextProvider fallback) {
        this.primary = primary;
        this.fallback = fallback;
        // 默认先标成 Keyword，避免尚未 retrieve 时误报 PGVECTOR
        this.lastSource = ThreadLocal.withInitial(fallback::sourceLabel);
    }

    @Override
    public Optional<String> retrieve(RetrievalQuery query, int budgetChars) {
        try {
            Optional<String> hit = primary.retrieve(query, budgetChars);
            if (hit.isPresent()) {
                lastSource.set(primary.sourceLabel());
                return hit;
            }
        } catch (Exception ex) {
            // 向量链路任何失败都不阻断对话：降级 Keyword，保证 L2 仍可能有一点上下文
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
