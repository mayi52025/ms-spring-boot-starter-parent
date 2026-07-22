package com.ms.middleware.console.agent.context;

import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * L2 检索的 Spring 装配（与是否开启 RAG 解耦）。
 *
 * <ul>
 *   <li>Keyword 实现类<strong>始终</strong>注册（有 Insight 就有）——RAG 开时给 Composite 当 fallback</li>
 *   <li>{@link RetrievalContextProvider} SPI：仅当 {@code rag.enabled=false}（默认）时，Keyword 自己顶上</li>
 *   <li>{@code rag.enabled=true} 时：SPI 由 {@code RagAutoConfiguration} 里的 Composite{@code @Primary} 提供</li>
 * </ul>
 *
 * <p>Keyword 故意不用 {@code @Component}：否则会和这里的 {@code @Bean}、以及 Composite 再注册一次 SPI，
 * 造成「两个 RetrievalContextProvider」注入歧义。
 */
@Configuration(proxyBeanMethods = false)
public class RetrievalAutoConfiguration {

    /** 降级实现本体；rag 开/关都需要这个 Bean */
    @Bean
    @ConditionalOnBean(MiddlewareInsightService.class)
    public KeywordFallbackRetrievalProvider keywordFallbackRetrievalProvider(
            MiddlewareInsightService insightService) {
        return new KeywordFallbackRetrievalProvider(insightService);
    }

    /**
     * 默认路径：未开 RAG 时，Keyword 就是唯一的 {@link RetrievalContextProvider}。
     * matchIfMissing=true → 没配 rag.enabled 也走这里。
     */
    @Bean
    @ConditionalOnBean(KeywordFallbackRetrievalProvider.class)
    @ConditionalOnProperty(
            prefix = "ms.middleware.console.rag",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true)
    public RetrievalContextProvider retrievalContextProvider(
            KeywordFallbackRetrievalProvider keywordFallbackRetrievalProvider) {
        return keywordFallbackRetrievalProvider;
    }
}
