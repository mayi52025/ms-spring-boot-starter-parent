package com.ms.middleware.console.agent.rag;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 短 TTL + 有界 LRU 的 embedding 缓存，降低对话热路径重复打通义的延迟与费用。
 * <p>索引写入路径也会受益（同一 chunk 重试）；容量故意做小，避免占满堆。
 */
public class CachingEmbeddingClient implements EmbeddingClient {

    private final EmbeddingClient delegate;
    private final int maxEntries;
    private final long ttlMillis;
    private final Object lock = new Object();
    private final Map<String, CacheEntry> cache;

    public CachingEmbeddingClient(EmbeddingClient delegate, int maxEntries, long ttlMillis) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.maxEntries = Math.max(8, maxEntries);
        this.ttlMillis = Math.max(1_000L, ttlMillis);
        this.cache = new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > CachingEmbeddingClient.this.maxEntries;
            }
        };
    }

    @Override
    public float[] embed(String text) {
        String key = text == null ? "" : text.trim();
        long now = System.currentTimeMillis();
        synchronized (lock) {
            CacheEntry hit = cache.get(key);
            if (hit != null && now - hit.storedAtMillis <= ttlMillis) {
                return Arrays.copyOf(hit.vector, hit.vector.length);
            }
        }
        float[] vector = delegate.embed(text);
        synchronized (lock) {
            cache.put(key, new CacheEntry(Arrays.copyOf(vector, vector.length), now));
        }
        return vector;
    }

    private record CacheEntry(float[] vector, long storedAtMillis) {
    }
}
