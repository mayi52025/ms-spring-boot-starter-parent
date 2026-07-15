package com.ms.middleware.console.agent.context;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * run 快照短 TTL 缓存：同一 run 连续追问时避免重复 format timeline。
 */
@Component
public class RunContextCache {

    private final ConcurrentHashMap<String, CachedEntry> cache = new ConcurrentHashMap<>();

    public Optional<RunContextSnapshot> get(String runId, int ttlSeconds, Supplier<Optional<RunContextSnapshot>> loader) {
        if (runId == null || runId.isBlank()) {
            return loader.get();
        }
        CachedEntry entry = cache.get(runId);
        Instant now = Instant.now();
        if (entry != null && entry.expiresAt.isAfter(now)) {
            return Optional.of(entry.snapshot);
        }
        Optional<RunContextSnapshot> loaded = loader.get();
        loaded.ifPresent(snapshot -> cache.put(runId, new CachedEntry(
                snapshot,
                now.plusSeconds(Math.max(5, ttlSeconds)))));
        return loaded;
    }

    public void invalidate(String runId) {
        if (runId != null) {
            cache.remove(runId);
        }
    }

    private record CachedEntry(RunContextSnapshot snapshot, Instant expiresAt) {
    }
}
