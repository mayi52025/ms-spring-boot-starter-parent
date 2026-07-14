package com.ms.middleware.autonomy.adoption.nacos;

import java.util.Optional;

/** audit-only 模式：不创建、不发布 Nacos 草稿 */
enum NoopNacosConfigDraftService implements NacosConfigDraftService {

    INSTANCE;

    @Override
    public boolean isDraftModeEnabled() {
        return false;
    }

    @Override
    public Optional<NacosConfigDraft> createDraft(NacosDraftRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<NacosConfigDraft> publishDraft(String draftId) {
        return Optional.empty();
    }

    @Override
    public Optional<NacosConfigDraft> findDraft(String draftId) {
        return Optional.empty();
    }
}
