package com.ms.middleware.autonomy.adoption;

import com.ms.middleware.autonomy.plan.RecommendationStatus;

/**
 * 采纳/拒绝 API 的统一响应，便于控制台展示与幂等重试判断。
 */
public class AdoptionResult {

    private boolean success;
    /** 业务码：OK / ALREADY_ACCEPTED / ALREADY_REJECTED / NOT_FOUND / CONFLICT */
    private String code;
    private String message;
    private String runId;
    private String recommendationId;
    private RecommendationStatus status;
    /** 人工采纳备选动作时返回（rank≥2 的 ADVISE 动作） */
    private Integer actionRank;
    /** nacos-draft：草稿 ID */
    private String draftId;
    /** nacos-draft：diff 摘要 */
    private String diffSummary;
    /** nacos-draft：是否已发布生产 */
    private Boolean nacosPublished;

    public static AdoptionResult ok(String runId, String recommendationId, RecommendationStatus status, String message) {
        AdoptionResult r = new AdoptionResult();
        r.success = true;
        r.code = "OK";
        r.runId = runId;
        r.recommendationId = recommendationId;
        r.status = status;
        r.message = message;
        return r;
    }

    public static AdoptionResult actionOk(String runId, int rank, String message) {
        AdoptionResult r = new AdoptionResult();
        r.success = true;
        r.code = "OK";
        r.runId = runId;
        r.actionRank = rank;
        r.message = message;
        return r;
    }

    public static AdoptionResult idempotent(String runId, String recommendationId,
                                            RecommendationStatus status, String message) {
        AdoptionResult r = ok(runId, recommendationId, status, message);
        r.code = status == RecommendationStatus.ACCEPTED ? "ALREADY_ACCEPTED" : "ALREADY_REJECTED";
        return r;
    }

    /** nacos-draft 二次发布成功 */
    public static AdoptionResult publishOk(String runId, String recommendationId, String draftId,
                                           String diffSummary, String message) {
        AdoptionResult r = ok(runId, recommendationId, RecommendationStatus.ACCEPTED, message);
        r.draftId = draftId;
        r.diffSummary = diffSummary;
        r.nacosPublished = true;
        return r;
    }

    /** 已发布，幂等 */
    public static AdoptionResult alreadyPublished(String runId, String recommendationId,
                                                  String draftId, String message) {
        AdoptionResult r = publishOk(runId, recommendationId, draftId, null, message);
        r.code = "ALREADY_PUBLISHED";
        return r;
    }

    public static AdoptionResult withDraft(AdoptionResult base, String draftId, String diffSummary) {
        base.draftId = draftId;
        base.diffSummary = diffSummary;
        base.nacosPublished = false;
        return base;
    }

    public static AdoptionResult fail(String code, String message) {
        AdoptionResult r = new AdoptionResult();
        r.success = false;
        r.code = code;
        r.message = message;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public Integer getActionRank() {
        return actionRank;
    }

    public void setActionRank(Integer actionRank) {
        this.actionRank = actionRank;
    }

    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }

    public String getDiffSummary() {
        return diffSummary;
    }

    public void setDiffSummary(String diffSummary) {
        this.diffSummary = diffSummary;
    }

    public Boolean getNacosPublished() {
        return nacosPublished;
    }

    public void setNacosPublished(Boolean nacosPublished) {
        this.nacosPublished = nacosPublished;
    }
}
