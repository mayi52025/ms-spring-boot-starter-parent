package com.ms.middleware.autonomy.adoption.nacos;

import com.ms.middleware.MsMiddlewareProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内存 Nacos 草稿：创建、发布与幂等。
 */
class InMemoryNacosConfigDraftServiceTest {

    private InMemoryNacosConfigDraftService service;

    @BeforeEach
    void setUp() {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        properties.getAutonomy().getAdoption().setMode("nacos-draft");
        service = new InMemoryNacosConfigDraftService(properties);
    }

    @Test
    void createDraftReturnsDiffSummary() {
        NacosDraftRequest request = new NacosDraftRequest();
        request.setApplicationName("order-system");
        request.setRecommendationId("abc12345");
        request.setSuggestedConfig("ms.middleware.autonomy.mq.throttle-limit=50");

        Optional<NacosConfigDraft> draft = service.createDraft(request);

        assertTrue(draft.isPresent());
        assertEqualsSafe("abc12345", draft.get().getDraftId());
        assertNotNull(draft.get().getDiffSummary());
        assertTrue(draft.get().getDiffSummary().contains("throttle-limit=50"));
        assertFalse(draft.get().isPublished());
    }

    @Test
    void publishDraftMarksPublished() {
        NacosDraftRequest request = new NacosDraftRequest();
        request.setApplicationName("demo-app");
        request.setRecommendationId("draft001");
        request.setSuggestedConfig("key: value");
        service.createDraft(request);

        Optional<NacosConfigDraft> published = service.publishDraft("draft001");

        assertTrue(published.isPresent());
        assertTrue(published.get().isPublished());
        assertNotNull(published.get().getPublishedAt());
    }

    @Test
    void publishDraftIsIdempotent() {
        NacosDraftRequest request = new NacosDraftRequest();
        request.setApplicationName("demo-app");
        request.setRecommendationId("draft002");
        request.setSuggestedConfig("a: 1");
        service.createDraft(request);
        service.publishDraft("draft002");

        Optional<NacosConfigDraft> second = service.publishDraft("draft002");
        assertTrue(second.isPresent());
        assertTrue(second.get().isPublished());
    }

    private static void assertEqualsSafe(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
