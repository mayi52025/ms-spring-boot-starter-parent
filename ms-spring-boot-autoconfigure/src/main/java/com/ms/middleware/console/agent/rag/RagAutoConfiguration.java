package com.ms.middleware.console.agent.rag;

import com.ms.middleware.MsMiddlewareProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;

/**
 * Phase 5.4：仅在 {@code ms.middleware.console.rag.enabled=true} 时装配专用 DataSource。
 * 不标记 {@code @Primary}，避免抢走业务库。
 */
@Configuration(proxyBeanMethods = false)
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
        // 启动时 PG 不可达不拖垮整个应用上下文；ensureSchema 在 Ready 时再试
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
    public RagSchemaInitializer ragSchemaInitializer(RagVectorStore ragVectorStore) {
        return new RagSchemaInitializer(ragVectorStore);
    }

    /** 应用 Ready 后建表，避免 Bean 创建阶段连库失败被 IDE/启动误判为硬错误 */
    public static final class RagSchemaInitializer {

        private final RagVectorStore store;

        public RagSchemaInitializer(RagVectorStore store) {
            this.store = store;
        }

        @EventListener(ApplicationReadyEvent.class)
        public void onReady() {
            try {
                store.ensureSchema();
            } catch (Exception ex) {
                log.warn("RAG ensureSchema failed (pgvector 未就绪时忽略): {}", ex.getMessage());
            }
        }
    }
}
