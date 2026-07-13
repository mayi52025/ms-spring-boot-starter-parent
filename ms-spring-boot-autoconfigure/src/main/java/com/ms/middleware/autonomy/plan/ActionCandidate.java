package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.AutonomyActionType;

/**
 * 排序前的动作候选，由 {@link IncidentActionCatalog} 按 incident 类型产出。
 *
 * <p>经 {@link ActionSelector} 按 Runbook 规则选优后，转为带 rank/confidence 的 {@link PlannedAction}。</p>
 */
public class ActionCandidate {

    /** 候选动作类型 */
    private AutonomyActionType actionType;
    /**
     * Runbook 顺序（越小越优先）。
     * 同 incident 下 SRE 手册规定的处置先后，不是浮点权重。
     */
    private int runbookOrder;
    /**
     * 是否针对根因（如触发自愈），词典序选优时优先于仅缓解症状的动作。
     */
    private boolean addressesRootCause;
    /** 人类可读的候选理由，写入 PLAN 时间线与选优说明 */
    private String reason;

    public ActionCandidate() {
    }

    public ActionCandidate(AutonomyActionType actionType, int runbookOrder,
                           boolean addressesRootCause, String reason) {
        this.actionType = actionType;
        this.runbookOrder = runbookOrder;
        this.addressesRootCause = addressesRootCause;
        this.reason = reason;
    }

    /**
     * 工厂方法，便于在 {@link IncidentActionCatalog} 中声明候选。
     *
     * @param actionType          动作类型
     * @param runbookOrder        Runbook 顺序（越小越优先）
     * @param addressesRootCause  是否针对根因
     * @param reason              人类可读理由
     * @return 动作候选实例
     */
    public static ActionCandidate of(AutonomyActionType actionType, int runbookOrder,
                                     boolean addressesRootCause, String reason) {
        return new ActionCandidate(actionType, runbookOrder, addressesRootCause, reason);
    }

    public AutonomyActionType getActionType() {
        return actionType;
    }

    public void setActionType(AutonomyActionType actionType) {
        this.actionType = actionType;
    }

    public int getRunbookOrder() {
        return runbookOrder;
    }

    public void setRunbookOrder(int runbookOrder) {
        this.runbookOrder = runbookOrder;
    }

    public boolean isAddressesRootCause() {
        return addressesRootCause;
    }

    public void setAddressesRootCause(boolean addressesRootCause) {
        this.addressesRootCause = addressesRootCause;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
