package com.ms.middleware.console.agent.rag;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.run.AutonomyRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 索引 STABLE run 摘要与 classpath 运维文档。
 *
 * <p>文档走 {@link TextChunker}（分句 + 重叠），便于检索只命中核心段落、控制注入 token；
 * STABLE 摘要本身短，整段一条向量即可。
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
        int chunkSize = Math.max(120, ragProperties.getChunkSize());
        int overlap = Math.max(0, ragProperties.getChunkOverlap());
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
                // 重分块前清掉该文档旧 chunk，避免 chunk_no 变少后残留长块
                store.deleteByRef("_global_", RagDocumentKind.DOC, filename);
                List<String> chunks = TextChunker.chunk(body, chunkSize, overlap);
                for (int i = 0; i < chunks.size(); i++) {
                    String chunk = chunks.get(i);
                    float[] vector = embeddingClient.embed(chunk);
                    store.upsert("_global_", RagDocumentKind.DOC, filename, i, chunk, vector);
                    indexed++;
                }
            }
            log.info("RAG indexed classpath docs chunks={} chunkSize={} overlap={}",
                    indexed, chunkSize, overlap);
        } catch (Exception ex) {
            log.warn("RAG indexClasspathDocs failed: {}", ex.getMessage());
        }
    }

    /** @deprecated 请用 {@link TextChunker#chunk(String, int, int)}；保留给旧单测兼容 */
    static List<String> chunk(String text, int chunkSize) {
        return TextChunker.chunk(text, chunkSize, 0);
    }
}
