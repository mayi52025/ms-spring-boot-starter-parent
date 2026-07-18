package com.ms.middleware.console.agent.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * pgvector 存储：启动时 ensureSchema；Step 2/3 再补 upsert/search。
 *
 * <p>SQL 使用普通字符串拼接，避免 IDE 对 text block 的误报。</p>
 */
public class RagVectorStore {

    private static final Logger log = LoggerFactory.getLogger(RagVectorStore.class);

    public static final String TABLE = "ms_rag_document";

    private final JdbcTemplate jdbc;
    private final int dimensions;

    public RagVectorStore(DataSource dataSource, int dimensions) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.dimensions = Math.max(8, dimensions);
    }

    public int dimensions() {
        return dimensions;
    }

    public JdbcTemplate jdbcTemplate() {
        return jdbc;
    }

    /**
     * 创建 extension + 表。幂等；换 dimensions 需手工清库（一期不自动 ALTER）。
     */
    public void ensureSchema() {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "id BIGSERIAL PRIMARY KEY, "
                + "tenant VARCHAR(128) NOT NULL, "
                + "kind VARCHAR(16) NOT NULL, "
                + "ref_id VARCHAR(256) NOT NULL, "
                + "chunk_no INT NOT NULL DEFAULT 0, "
                + "content TEXT NOT NULL, "
                + "embedding vector(" + dimensions + ") NOT NULL, "
                + "created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), "
                + "UNIQUE (tenant, kind, ref_id, chunk_no)"
                + ")";
        jdbc.execute(createTable);
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_ms_rag_document_tenant_kind ON "
                + TABLE + " (tenant, kind)");
        log.info("RAG schema ready: table={} dimensions={}", TABLE, dimensions);
    }

    /** 供单测 / 诊断：当前表是否存在 */
    public boolean tableExists() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = ?",
                Integer.class,
                TABLE);
        return n != null && n > 0;
    }
}
