package com.ms.middleware.autonomy.adoption.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.config.ConfigCenterAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 真实 Nacos 草稿服务：draft 写入独立 dataId，二次确认后才 publish 到生产 dataId。
 */
public class NacosConfigDraftServiceImpl implements NacosConfigDraftService {

    private static final Logger logger = LoggerFactory.getLogger(NacosConfigDraftServiceImpl.class);

    private final ConfigCenterAutoConfiguration.ConfigCenterClient configCenterClient;
    private final MsMiddlewareProperties.AdoptionProperties adoptionProperties;
    private final Environment environment;
    /** 本地索引：draftId → meta（Nacos 上仅存 content） */
    private final Map<String, NacosConfigDraft> draftIndex = new ConcurrentHashMap<>();

    public NacosConfigDraftServiceImpl(ConfigCenterAutoConfiguration.ConfigCenterClient configCenterClient,
                                       MsMiddlewareProperties properties,
                                       Environment environment) {
        this.configCenterClient = configCenterClient;
        this.adoptionProperties = properties.getAutonomy().getAdoption();
        this.environment = environment;
    }

    @Override
    public boolean isDraftModeEnabled() {
        return adoptionProperties.isNacosDraftMode();
    }

    @Override
    public Optional<NacosConfigDraft> createDraft(NacosDraftRequest request) {
        try {
            String appName = resolveAppName(request.getApplicationName());
            String productionDataId = resolveProductionDataId(appName);
            String draftDataId = appName + adoptionProperties.getDraftDataIdSuffix()
                    + "-" + request.getRecommendationId() + ".yaml";
            String group = adoptionProperties.getDraftGroup();

            String current = configCenterClient.getConfig(productionDataId, group, 3000);
            String[] built = NacosConfigDraftContentBuilder.build(current, request.getSuggestedConfig());

            boolean ok = configCenterClient.publishConfig(draftDataId, group, built[0]);
            if (!ok) {
                logger.warn("Nacos draft publish 返回 false draftDataId={}", draftDataId);
                return Optional.empty();
            }

            NacosConfigDraft draft = new NacosConfigDraft();
            draft.setDraftId(request.getRecommendationId());
            draft.setDraftDataId(draftDataId);
            draft.setProductionDataId(productionDataId);
            draft.setGroup(group);
            draft.setDraftContent(built[0]);
            draft.setDiffSummary(built[1]);
            draft.setPublished(false);
            draft.setCreatedAt(Instant.now());
            draftIndex.put(draft.getDraftId(), draft);

            logger.info("Nacos 草稿已写入 draftDataId={} productionDataId={} draftId={}",
                    draftDataId, productionDataId, draft.getDraftId());
            return Optional.of(draft);
        } catch (NacosException e) {
            logger.warn("创建 Nacos 草稿失败 recommendationId={}: {}", request.getRecommendationId(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<NacosConfigDraft> publishDraft(String draftId) {
        NacosConfigDraft draft = draftIndex.get(draftId);
        if (draft == null) {
            return Optional.empty();
        }
        if (draft.isPublished()) {
            return Optional.of(draft);
        }
        try {
            boolean ok = configCenterClient.publishConfig(
                    draft.getProductionDataId(), draft.getGroup(), draft.getDraftContent());
            if (!ok) {
                logger.warn("Nacos 生产 publish 返回 false dataId={}", draft.getProductionDataId());
                return Optional.empty();
            }
            draft.setPublished(true);
            draft.setPublishedAt(Instant.now());
            logger.info("Nacos 草稿已确认发布到生产 dataId={} draftId={}",
                    draft.getProductionDataId(), draftId);
            return Optional.of(draft);
        } catch (NacosException e) {
            logger.warn("Nacos 草稿发布失败 draftId={}: {}", draftId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<NacosConfigDraft> findDraft(String draftId) {
        return Optional.ofNullable(draftIndex.get(draftId));
    }

    private String resolveAppName(String fromRequest) {
        if (fromRequest != null && !fromRequest.isBlank()) {
            return fromRequest.trim();
        }
        return environment.getProperty("spring.application.name", "default-app");
    }

    private String resolveProductionDataId(String appName) {
        String suffix = adoptionProperties.getProductionDataIdSuffix();
        if (suffix == null || suffix.isBlank()) {
            suffix = ".yaml";
        }
        if (!suffix.startsWith(".")) {
            suffix = "." + suffix;
        }
        return appName + suffix;
    }
}
