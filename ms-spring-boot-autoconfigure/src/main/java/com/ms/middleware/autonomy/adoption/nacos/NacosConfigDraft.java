package com.ms.middleware.autonomy.adoption.nacos;

import java.time.Instant;

/**
 * Nacos 配置草稿：采纳推荐时生成，需二次「确认发布」才写入生产 dataId。
 */
public class NacosConfigDraft {

    /** 草稿 ID，默认与 recommendationId 一致，便于控制台关联 */
    private String draftId;
    /** Nacos draft dataId（非生产） */
    private String draftDataId;
    /** Nacos 生产 dataId，确认发布时写入 */
    private String productionDataId;
    private String group;
    /** 合并后的草稿全文（供 diff 展示） */
    private String draftContent;
    /** 人类可读的 diff 摘要，如 {@code + ms.middleware...} */
    private String diffSummary;
    /** 是否已发布到生产 dataId */
    private boolean published;
    private Instant createdAt;
    private Instant publishedAt;

    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }

    public String getDraftDataId() {
        return draftDataId;
    }

    public void setDraftDataId(String draftDataId) {
        this.draftDataId = draftDataId;
    }

    public String getProductionDataId() {
        return productionDataId;
    }

    public void setProductionDataId(String productionDataId) {
        this.productionDataId = productionDataId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDraftContent() {
        return draftContent;
    }

    public void setDraftContent(String draftContent) {
        this.draftContent = draftContent;
    }

    public String getDiffSummary() {
        return diffSummary;
    }

    public void setDiffSummary(String diffSummary) {
        this.diffSummary = diffSummary;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
