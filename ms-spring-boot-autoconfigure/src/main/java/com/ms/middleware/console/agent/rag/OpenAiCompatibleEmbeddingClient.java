package com.ms.middleware.console.agent.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.MsMiddlewareProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI 兼容 {@code POST /embeddings}（通义百炼 DashScope compatible-mode 等）。
 */
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleEmbeddingClient.class);

    private final MsMiddlewareProperties.RagEmbeddingProperties config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleEmbeddingClient(MsMiddlewareProperties.RagEmbeddingProperties config,
                                           ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        int timeout = Math.max(5, config.getTimeoutSeconds());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("embed text blank");
        }
        String apiKey = config.resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("RAG embedding api-key empty (set MS_RAG_EMBEDDING_API_KEY)");
        }
        String base = config.getBaseUrl() == null ? "" : config.getBaseUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            throw new IllegalStateException("RAG embedding base-url empty");
        }
        String url = base + "/embeddings";

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModel());
            body.put("input", text);
            body.put("dimensions", config.getDimensions());

            byte[] payload = objectMapper.writeValueAsBytes(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(5, config.getTimeoutSeconds())))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("embedding HTTP " + response.statusCode() + ": "
                        + truncate(response.body(), 200));
            }
            return parseEmbedding(response.body());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("embedding request failed: {}", ex.getMessage());
            throw new IllegalStateException("embedding failed: " + ex.getMessage(), ex);
        }
    }

    private float[] parseEmbedding(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new IllegalStateException("embedding response missing data[]");
        }
        JsonNode emb = data.get(0).path("embedding");
        if (!emb.isArray() || emb.isEmpty()) {
            throw new IllegalStateException("embedding response missing data[0].embedding");
        }
        float[] vector = new float[emb.size()];
        for (int i = 0; i < emb.size(); i++) {
            vector[i] = (float) emb.get(i).asDouble();
        }
        return vector;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
