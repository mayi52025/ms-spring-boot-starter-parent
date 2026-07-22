package com.ms.middleware.console.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CachingEmbeddingClientTest {

    @Test
    void secondCallHitsCache_withinTtl() {
        AtomicInteger calls = new AtomicInteger();
        EmbeddingClient raw = text -> {
            calls.incrementAndGet();
            return new float[]{1f, 2f, 3f};
        };
        CachingEmbeddingClient cached = new CachingEmbeddingClient(raw, 16, 60_000L);

        float[] a = cached.embed("tick 锁");
        float[] b = cached.embed("tick 锁");

        assertEquals(1, calls.get());
        assertArrayEquals(a, b);
    }

    @Test
    void differentTextMissesCache() {
        AtomicInteger calls = new AtomicInteger();
        EmbeddingClient raw = text -> {
            calls.incrementAndGet();
            return new float[]{(float) calls.get()};
        };
        CachingEmbeddingClient cached = new CachingEmbeddingClient(raw, 16, 60_000L);

        cached.embed("a");
        cached.embed("b");

        assertEquals(2, calls.get());
    }
}
