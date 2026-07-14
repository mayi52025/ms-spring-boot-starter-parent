package com.ms.middleware.autonomy.adoption.nacos;

import com.ms.middleware.MsMiddlewareProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存模拟 Nacos 草稿：Demo 未开 {@code ms.middleware.config.enabled} 时仍可走 nacos-draft 流程验收。
 *
 * <p>草稿与「生产」均存在 JVM 内存 Map，不触达真实 Nacos。</p>
 */
public class InMemoryNacosConfigDraftService implements NacosConfigDraftService {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryNacosConfigDraftService.class);

    private final MsMiddlewareProperties.AdoptionProperties adoptionProperties;
    /** draftId → 草稿 */
    private final Map<String, NacosConfigDraft> drafts = new ConcurrentHashMap<>();
    /** productionDataId → 已发布内容（模拟生产 Nacos） */
    private final Map<String, String> productionStore = new ConcurrentHashMap<>();

    public InMemoryNacosConfigDraftService(MsMiddlewareProperties properties) {
        this.adoptionProperties = properties.getAutonomy().getAdoption();
    }

    @Override
    public boolean isDraftModeEnabled() {
        return adoptionProperties.isNacosDraftMode();
    }

    @Override
    public Optional<NacosConfigDraft> createDraft(NacosDraftRequest request) {
        String appName = safeAppName(request.getApplicationName());
        String productionDataId = appName + adoptionProperties.getProductionDataIdSuffix();
        String draftDataId = appName + adoptionProperties.getDraftDataIdSuffix()
                + "-" + request.getRecommendationId() + ".yaml";
        String current = productionStore.get(productionDataId);
        String[] built = NacosConfigDraftContentBuilder.build(current, request.getSuggestedConfig());

        NacosConfigDraft draft = new NacosConfigDraft();
        draft.setDraftId(request.getRecommendationId());
        draft.setDraftDataId(draftDataId);
        draft.setProductionDataId(productionDataId);
        draft.setGroup(adoptionProperties.getDraftGroup());
        draft.setDraftContent(built[0]);
        draft.setDiffSummary(built[1]);
        draft.setPublished(false);
        draft.setCreatedAt(Instant.now());
        drafts.put(draft.getDraftId(), draft);

        logger.info("内存 Nacos 草稿已创建 draftId={} draftDataId={}（模拟，未发布生产）",
                draft.getDraftId(), draft.getDraftDataId());
        return Optional.of(draft);
    }

    @Override
    public Optional<NacosConfigDraft> publishDraft(String draftId) {
        NacosConfigDraft draft = drafts.get(draftId);
        if (draft == null) {
            return Optional.empty();
        }
        if (draft.isPublished()) {
            return Optional.of(draft);
        }
        productionStore.put(draft.getProductionDataId(), draft.getDraftContent());
        draft.setPublished(true);
        draft.setPublishedAt(Instant.now());
        logger.info("内存 Nacos 草稿已发布到生产 dataId={} draftId={}",
                draft.getProductionDataId(), draftId);
        return Optional.of(draft);
    }

    @Override
    public Optional<NacosConfigDraft> findDraft(String draftId) {
        return Optional.ofNullable(drafts.get(draftId));
    }

    private static String safeAppName(String appName) {
        if (appName == null || appName.isBlank()) {
            return "default-app";
        }
        return appName.trim();
    }
}
