package com.ms.middleware.console.agent.rag;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.run.AutonomyRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 索引 STABLE run 摘要与 classpath 运维文档。
 */
public class RagIndexer {

    private static final Logger log = LoggerFactory.getLogger(RagIndexer.class);
    private static final String DOC_PATTERN = "classpath*:rag/docs/**/*.md";

    private final EmbeddingClient embeddingClient;
    private final RagVectorStore store;
    private final MsMiddlewareProperties.RagProperties ragProperties;

    public RagIndexer(EmbeddingClient embeddingClient,
                      RagVectorStore store,
                      MsMiddlewareProperties.RagProperties ragProperties) {
        this.embeddingClient = embeddingClient;
        this.store = store;
        this.ragProperties = ragProperties;
    }

    public void indexStableRun(AutonomyRun run) {
        if (run == null || run.getRunId() == null || run.getRunId().isBlank()) {
            return;
        }
        String text = StableRunTextBuilder.build(run);
        if (text.isBlank()) {
            return;
        }
        float[] vector = embeddingClient.embed(text);
        String tenant = run.getTenant() != null ? run.getTenant() : "";
        store.upsert(tenant, RagDocumentKind.RUN, run.getRunId(), 0, text, vector);
        int deleted = store.trimOldRuns(tenant, ragProperties.getMaxRunDocsPerTenant());
        log.info("RAG indexed STABLE runId={} tenant={} trimmed={}", run.getRunId(), tenant, deleted);
    }

    public void indexClasspathDocs() {
        int chunkSize = Math.max(200, ragProperties.getChunkSize());
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(DOC_PATTERN);
            int indexed = 0;
            for (Resource resource : resources) {
                if (!resource.exists() || !resource.isReadable()) {
                    continue;
                }
                String filename = resource.getFilename() != null ? resource.getFilename() : "doc.md";
                String body = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                List<String> chunks = chunk(body, chunkSize);
                for (int i = 0; i < chunks.size(); i++) {
                    String chunk = chunks.get(i);
                    float[] vector = embeddingClient.embed(chunk);
                    store.upsert("_global_", RagDocumentKind.DOC, filename, i, chunk, vector);
                    indexed++;
                }
            }
            log.info("RAG indexed classpath docs chunks={}", indexed);
        } catch (Exception ex) {
            log.warn("RAG indexClasspathDocs failed: {}", ex.getMessage());
        }
    }

    static List<String> chunk(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String normalized = text.replace("\r\n", "\n").trim();
        if (normalized.length() <= chunkSize) {
            chunks.add(normalized);
            return chunks;
        }
        int from = 0;
        while (from < normalized.length()) {
            int to = Math.min(normalized.length(), from + chunkSize);
            if (to < normalized.length()) {
                int breakAt = normalized.lastIndexOf('\n', to);
                if (breakAt > from + chunkSize / 2) {
                    to = breakAt;
                }
            }
            String piece = normalized.substring(from, to).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            from = Math.max(to, from + 1);
        }
        return chunks;
    }
}
