package com.ms.middleware.autonomy.adoption;

/**
 * 人工采纳/拒绝 API 的请求体。
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>{@code runId} — 可选；不传则在当前租户最近 run 中按 recommendationId 查找</li>
 *   <li>{@code operator} — 操作人标识，写入审计时间线（如工号、用户名）</li>
 *   <li>{@code comment} — 可选备注</li>
 * </ul>
 */
public class AdoptionRequest {

    /** 可选；不传则在当前租户最近 run 中按 recommendationId 查找 */
    private String runId;
    /** 操作人标识，写入审计时间线（如工号、用户名） */
    private String operator;
    /** 可选备注 */
    private String comment;
    /** 请求来源 IP，由控制台 API 从 HttpServletRequest 注入，写入时间线审计 */
    private String clientIp;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
}
