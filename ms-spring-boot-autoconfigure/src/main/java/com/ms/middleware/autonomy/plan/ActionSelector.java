package com.ms.middleware.autonomy.plan;

import com.ms.middleware.autonomy.context.AutonomyContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 规则选优器（自治「思考层」）：用 Runbook 词典序挑选动作，不用浮点加权打分。
 *
 * <p>选优规则（逐级比较，前一项能分出胜负则不看下一项）：</p>
 * <ol>
 *   <li>是否针对根因（{@link ActionCandidate#isAddressesRootCause()}，是者优先）</li>
 *   <li>风险从低到高（LOW &lt; MEDIUM &lt; HIGH）</li>
 *   <li>Runbook 顺序（{@link ActionCandidate#getRunbookOrder()} 越小越优先）</li>
 * </ol>
 *
 * <p>证据强度（confidence）由 {@link EvidenceStrengthEvaluator} 独立计算，供 {@link com.ms.middleware.autonomy.policy.AutonomyPolicy} 门控。</p>
 */
public class ActionSelector {

    /** 词典序比较器：根因 &gt; 低风险 &gt; Runbook 顺序 */
    private static final Comparator<ActionCandidate> LEXICOGRAPHIC_ORDER = Comparator
            .comparing(ActionCandidate::isAddressesRootCause).reversed()
            .thenComparingInt(c -> c.getActionType().getRisk().ordinal())
            .thenComparingInt(ActionCandidate::getRunbookOrder);

    /**
     * 从候选池按规则选优，输出带 rank/confidence 的 {@link PlannedAction} 列表。
     *
     * @param candidates 同 incident 下的动作候选
     * @param context    当前快照，仅用于证据强度评估
     * @return rank 从 1 开始的计划动作列表
     */
    public List<PlannedAction> select(List<ActionCandidate> candidates, AutonomyContext context) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<ActionCandidate> ordered = new ArrayList<>(candidates);
        ordered.sort(LEXICOGRAPHIC_ORDER);

        List<PlannedAction> selected = new ArrayList<>();
        int rank = 1;
        for (ActionCandidate candidate : ordered) {
            selected.add(toPlannedAction(candidate, rank, context));
            rank++;
        }
        return selected;
    }

    /**
     * 生成 PLAN 时间线可读的选优说明，解释「为什么选第一名」。
     *
     * @param selectedActions 已选优并排好序的动作列表
     * @return 可写入时间线的选优说明文本
     */
    public String buildSelectionSummary(List<PlannedAction> selectedActions) {
        if (selectedActions == null || selectedActions.isEmpty()) {
            return "无自动动作候选，仅输出配置推荐";
        }

        StringBuilder sb = new StringBuilder(
                "选优依据：根因优先 > 风险从低到高 > Runbook 顺序。动作序列：");
        for (int i = 0; i < selectedActions.size(); i++) {
            PlannedAction action = selectedActions.get(i);
            if (i > 0) {
                sb.append("；");
            }
            sb.append(String.format(Locale.ROOT, "#%d %s（证据强度 %.2f）— %s",
                    action.getRank(),
                    action.getActionType(),
                    action.getConfidence(),
                    action.getReason()));
        }

        PlannedAction top = selectedActions.get(0);
        sb.append(String.format(Locale.ROOT,
                "。首选 #1 %s 将优先自动执行（LOW 风险止血）；其余为备选方案。",
                top.getActionType()));
        return sb.toString();
    }

    private PlannedAction toPlannedAction(ActionCandidate candidate, int rank, AutonomyContext context) {
        PlannedAction action = new PlannedAction();
        action.setActionType(candidate.getActionType());
        action.setRisk(candidate.getActionType().getRisk());
        action.setReason(candidate.getReason());
        action.setRank(rank);
        // score 字段保留给 JSON 契约，规则选优不使用浮点得分
        action.setScore(0);
        action.setConfidence(EvidenceStrengthEvaluator.evaluate(candidate.getActionType(), context));
        return action;
    }
}
