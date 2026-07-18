package com.ms.middleware.console.agent.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Locale;

/**
 * pgvector 存储：建表、upsert、按 tenant 裁剪旧 RUN。
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

    public void upsert(String tenant,
                       RagDocumentKind kind,
                       String refId,
                       int chunkNo,
                       String content,
                       float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("embedding empty");
        }
        String sql = "INSERT INTO " + TABLE
                + " (tenant, kind, ref_id, chunk_no, content, embedding) "
                + "VALUES (?, ?, ?, ?, ?, ?::vector) "
                + "ON CONFLICT (tenant, kind, ref_id, chunk_no) DO UPDATE SET "
                + "content = EXCLUDED.content, "
                + "embedding = EXCLUDED.embedding, "
                + "created_at = NOW()";
        jdbc.update(sql,
                nullToEmpty(tenant),
                kind.name(),
                nullToEmpty(refId),
                chunkNo,
                content == null ? "" : content,
                toVectorLiteral(embedding));
    }

    /**
     * 每 tenant 仅保留最近 {@code keep} 条 RUN 文档，超出删最旧。
     */
    public int trimOldRuns(String tenant, int keep) {
        int safeKeep = Math.max(1, keep);
        String sql = "DELETE FROM " + TABLE + " WHERE id IN ("
                + "SELECT id FROM " + TABLE
                + " WHERE tenant = ? AND kind = ? "
                + "ORDER BY created_at DESC OFFSET ?"
                + ")";
        Integer deleted = jdbc.update(sql, nullToEmpty(tenant), RagDocumentKind.RUN.name(), safeKeep);
        return deleted;
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

    static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 8);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.ROOT, "%.8f", embedding[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
