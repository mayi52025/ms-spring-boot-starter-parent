package com.ms.middleware.console.agent.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import com.ms.middleware.console.agent.context.CompositeRetrievalContextProvider;
import com.ms.middleware.console.agent.context.KeywordFallbackRetrievalProvider;
import com.ms.middleware.console.agent.context.RetrievalContextProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.Executor;

/**
 * Phase 5.4：仅在 {@code ms.middleware.console.rag.enabled=true} 时装配专用 DataSource、索引与异步监听。
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@ConditionalOnClass(name = {
        "javax.sql.DataSource",
        "com.zaxxer.hikari.HikariDataSource",
        "org.postgresql.Driver"
})
@ConditionalOnProperty(prefix = "ms.middleware.console.rag", name = "enabled", havingValue = "true")
public class RagAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RagAutoConfiguration.class);

    public static final String RAG_DATA_SOURCE = "ragDataSource";

    @Bean(name = RAG_DATA_SOURCE, destroyMethod = "close")
    public HikariDataSource ragDataSource(MsMiddlewareProperties properties) {
        MsMiddlewareProperties.RagProperties rag = properties.getConsole().getRag();
        HikariConfig config = new HikariConfig();
        config.setPoolName("ms-rag");
        config.setJdbcUrl(rag.getJdbcUrl());
        config.setUsername(rag.getUsername());
        config.setPassword(rag.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5_000L);
        config.setInitializationFailTimeout(-1);
        log.info("RAG DataSource configured: url={}", rag.getJdbcUrl());
        return new HikariDataSource(config);
    }

    @Bean
    public RagVectorStore ragVectorStore(
            @Qualifier(RAG_DATA_SOURCE) DataSource ragDataSource,
            MsMiddlewareProperties properties) {
        int dim = properties.getConsole().getRag().getEmbedding().getDimensions();
        return new RagVectorStore(ragDataSource, dim);
    }

    @Bean
    public EmbeddingClient ragEmbeddingClient(MsMiddlewareProperties properties, ObjectMapper objectMapper) {
        return new OpenAiCompatibleEmbeddingClient(properties.getConsole().getRag().getEmbedding(), objectMapper);
    }

    @Bean
    public RagIndexer ragIndexer(EmbeddingClient ragEmbeddingClient,
                                 RagVectorStore ragVectorStore,
                                 MsMiddlewareProperties properties) {
        return new RagIndexer(ragEmbeddingClient, ragVectorStore, properties.getConsole().getRag());
    }

    @Bean(name = "ragIndexExecutor")
    public Executor ragIndexExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("ms-rag-index-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RagIndexEventListener ragIndexEventListener(RagIndexer ragIndexer) {
        return new RagIndexEventListener(ragIndexer);
    }

    @Bean
    public RagBootstrap ragBootstrap(RagVectorStore ragVectorStore, RagIndexer ragIndexer) {
        return new RagBootstrap(ragVectorStore, ragIndexer);
    }

    /**
     * 向量检索实现（内部组件）。不直接标成对外 SPI，避免 ContextAssembler 注入到「半截链路」。
     */
    @Bean
    public PgvectorRetrievalContextProvider pgvectorRetrievalContextProvider(
            EmbeddingClient ragEmbeddingClient,
            RagVectorStore ragVectorStore,
            AutonomyTenantProvider tenantProvider,
            MsMiddlewareProperties properties) {
        return new PgvectorRetrievalContextProvider(
                ragEmbeddingClient,
                ragVectorStore,
                tenantProvider,
                properties.getConsole().getRag());
    }

    /**
     * rag.enabled=true 时：对外唯一 {@link RetrievalContextProvider}。
     * <p>@Primary：若将来还有别的同类型 Bean，注入优先选 Composite。
     * Keyword 仍单独存在，只作本 Composite 的 fallback 依赖。
     */
    @Bean
    @Primary
    @ConditionalOnBean(KeywordFallbackRetrievalProvider.class)
    public RetrievalContextProvider compositeRetrievalContextProvider(
            PgvectorRetrievalContextProvider pgvectorRetrievalContextProvider,
            KeywordFallbackRetrievalProvider keywordFallbackRetrievalProvider) {
        return new CompositeRetrievalContextProvider(
                pgvectorRetrievalContextProvider, keywordFallbackRetrievalProvider);
    }

    /** Ready 后建表并扫 classpath 文档 */
    public static final class RagBootstrap {

        private final RagVectorStore store;
        private final RagIndexer indexer;

        public RagBootstrap(RagVectorStore store, RagIndexer indexer) {
            this.store = store;
            this.indexer = indexer;
        }

        @EventListener(ApplicationReadyEvent.class)
        public void onReady() {
            try {
                store.ensureSchema();
                indexer.indexClasspathDocs();
            } catch (Exception ex) {
                log.warn("RAG bootstrap failed (pgvector/embedding 未就绪时忽略): {}", ex.getMessage());
            }
        }
    }
}
