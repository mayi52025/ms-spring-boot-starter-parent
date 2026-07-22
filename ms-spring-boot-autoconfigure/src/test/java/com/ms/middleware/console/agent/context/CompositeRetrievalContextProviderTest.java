package com.ms.middleware.console.agent.context;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeRetrievalContextProviderTest {

    @Test
    void usesPrimaryWhenHit_andSourceLabelIsPgvector() {
        RetrievalContextProvider primary = provider("PGVECTOR", Optional.of("pg-hit"));
        RetrievalContextProvider fallback = provider("KEYWORD_FALLBACK", Optional.of("kw-hit"));
        CompositeRetrievalContextProvider composite = new CompositeRetrievalContextProvider(primary, fallback);

        Optional<String> hit = composite.retrieve(
                new RetrievalQuery("上次类似故障", RetrievalQuery.RetrievalKind.HISTORICAL_RUN), 500);

        assertEquals("pg-hit", hit.orElseThrow());
        assertEquals("PGVECTOR", composite.sourceLabel());
    }

    @Test
    void fallsBackWhenPrimaryEmpty_andSourceLabelIsKeyword() {
        RetrievalContextProvider primary = provider("PGVECTOR", Optional.empty());
        RetrievalContextProvider fallback = provider("KEYWORD_FALLBACK", Optional.of("kw-hit"));
        CompositeRetrievalContextProvider composite = new CompositeRetrievalContextProvider(primary, fallback);

        Optional<String> hit = composite.retrieve(
                new RetrievalQuery("文档怎么写", RetrievalQuery.RetrievalKind.DOCUMENT), 500);

        assertEquals("kw-hit", hit.orElseThrow());
        assertEquals("KEYWORD_FALLBACK", composite.sourceLabel());
    }

    @Test
    void fallsBackWhenPrimaryThrows_andSourceLabelIsKeyword() {
        RetrievalContextProvider primary = new RetrievalContextProvider() {
            @Override
            public Optional<String> retrieve(RetrievalQuery query, int budgetChars) {
                throw new IllegalStateException("embedding down");
            }

            @Override
            public String sourceLabel() {
                return "PGVECTOR";
            }
        };
        RetrievalContextProvider fallback = provider("KEYWORD_FALLBACK", Optional.of("kw-hit"));
        CompositeRetrievalContextProvider composite = new CompositeRetrievalContextProvider(primary, fallback);

        Optional<String> hit = composite.retrieve(
                new RetrievalQuery("历史 run", RetrievalQuery.RetrievalKind.HISTORICAL_RUN), 500);

        assertEquals("kw-hit", hit.orElseThrow());
        assertEquals("KEYWORD_FALLBACK", composite.sourceLabel());
    }

    @Test
    void concurrentThreadsKeepIndependentSourceLabels() throws Exception {
        RetrievalContextProvider primary = new RetrievalContextProvider() {
            @Override
            public Optional<String> retrieve(RetrievalQuery query, int budgetChars) {
                if ("pg".equals(query.query())) {
                    return Optional.of("pg-hit");
                }
                return Optional.empty();
            }

            @Override
            public String sourceLabel() {
                return "PGVECTOR";
            }
        };
        RetrievalContextProvider fallback = provider("KEYWORD_FALLBACK", Optional.of("kw-hit"));
        CompositeRetrievalContextProvider composite = new CompositeRetrievalContextProvider(primary, fallback);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicReference<String> labelPg = new AtomicReference<>();
        AtomicReference<String> labelKw = new AtomicReference<>();

        Thread tPg = new Thread(() -> {
            ready.countDown();
            try {
                go.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            composite.retrieve(new RetrievalQuery("pg", RetrievalQuery.RetrievalKind.HISTORICAL_RUN), 100);
            labelPg.set(composite.sourceLabel());
        });
        Thread tKw = new Thread(() -> {
            ready.countDown();
            try {
                go.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            composite.retrieve(new RetrievalQuery("kw", RetrievalQuery.RetrievalKind.HISTORICAL_RUN), 100);
            labelKw.set(composite.sourceLabel());
        });
        tPg.start();
        tKw.start();
        ready.await();
        go.countDown();
        tPg.join();
        tKw.join();

        assertEquals("PGVECTOR", labelPg.get());
        assertEquals("KEYWORD_FALLBACK", labelKw.get());
        assertTrue(labelPg.get() != null && labelKw.get() != null);
    }

    private static RetrievalContextProvider provider(String label, Optional<String> result) {
        return new RetrievalContextProvider() {
            @Override
            public Optional<String> retrieve(RetrievalQuery query, int budgetChars) {
                return result;
            }

            @Override
            public String sourceLabel() {
                return label;
            }
        };
    }
}
