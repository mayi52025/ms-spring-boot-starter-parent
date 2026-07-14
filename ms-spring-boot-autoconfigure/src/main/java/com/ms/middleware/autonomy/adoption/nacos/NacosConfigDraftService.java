package com.ms.middleware.autonomy.adoption.nacos;

import java.util.Optional;

/**
 * Nacos 配置草稿服务：采纳推荐时写 draft dataId，二次确认后才 publish 到生产 dataId。
 *
 * <p>{@code ms.middleware.autonomy.adoption.mode=nacos-draft} 时启用；
 * 默认 {@code audit-only} 使用 {@link #noop()} 无操作实现。</p>
 */
public interface NacosConfigDraftService {

    /** 当前运行时是否处于 nacos-draft 模式 */
    boolean isDraftModeEnabled();

    /**
     * 根据 suggestedConfig 生成草稿并写入 Nacos draft dataId（或内存模拟）。
     * 不修改生产配置。
     */
    Optional<NacosConfigDraft> createDraft(NacosDraftRequest request);

    /**
     * 二次确认：将草稿内容 publish 到生产 dataId。
     */
    Optional<NacosConfigDraft> publishDraft(String draftId);

    Optional<NacosConfigDraft> findDraft(String draftId);

    /** audit-only 模式下的空实现 */
    static NacosConfigDraftService noop() {
        return NoopNacosConfigDraftService.INSTANCE;
    }
}
