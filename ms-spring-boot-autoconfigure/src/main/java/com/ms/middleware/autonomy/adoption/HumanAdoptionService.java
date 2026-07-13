package com.ms.middleware.autonomy.adoption;

import com.ms.middleware.autonomy.AutonomyPolicyDecision;
import com.ms.middleware.autonomy.act.AutonomyActuator;
import com.ms.middleware.autonomy.plan.AutonomyRecommendation;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.autonomy.plan.RecommendationStatus;
import com.ms.middleware.autonomy.run.AutonomyLedger;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.autonomy.run.AutonomyTimelinePhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;

/**
 * 人机协同采纳服务：配置推荐与备选 ADVISE 动作的审计 + 执行。
 *
 * <p>设计原则：</p>
 * <ul>
 *   <li>幂等：重复采纳返回 ALREADY_ACCEPTED，不重复写时间线</li>
 *   <li>可审计：每条决策写入 ACCEPTED phase，推荐类携带 recommendationId</li>
 *   <li>配置推荐仅记审计，不自动改 Nacos（Phase 4 再接真改配置）</li>
 *   <li>备选动作采纳后走 {@link AutonomyActuator} 真正执行，并补 AUTO 时间线</li>
 * </ul>
 */
public class HumanAdoptionService {

    private static final Logger logger = LoggerFactory.getLogger(HumanAdoptionService.class);

    private final AutonomyLedger ledger;
    private final AutonomyActuator actuator;

    public HumanAdoptionService(AutonomyLedger ledger, AutonomyActuator actuator) {
        this.ledger = ledger;
        this.actuator = actuator;
    }

    /**
     * 采纳配置级推荐。
     *
     * @param recommendationId 推荐 ID（8 位）
     * @param request            可选 runId、操作人、备注
     */
    public AdoptionResult acceptRecommendation(String recommendationId, AdoptionRequest request) {
        if (recommendationId == null || recommendationId.isBlank()) {
            return AdoptionResult.fail("INVALID", "recommendationId 不能为空");
        }
        Optional<ResolvedTarget> target = resolveRecommendation(recommendationId, request);
        if (target.isEmpty()) {
            return AdoptionResult.fail("NOT_FOUND", "未找到推荐 " + recommendationId);
        }
        AutonomyRun run = target.get().run();
        AutonomyRecommendation rec = target.get().recommendation();

        if (rec.getStatus() == RecommendationStatus.ACCEPTED) {
            return AdoptionResult.idempotent(run.getRunId(), recommendationId,
                    RecommendationStatus.ACCEPTED, "推荐已采纳，无需重复操作");
        }
        if (rec.getStatus() == RecommendationStatus.REJECTED) {
            return AdoptionResult.fail("CONFLICT", "该推荐已拒绝，不可再采纳");
        }

        rec.setStatus(RecommendationStatus.ACCEPTED);
        rec.setDecidedAt(Instant.now());
        rec.setOperator(resolveOperator(request));
        rec.setRejectReason(null);

        String message = buildRecommendationMessage("采纳推荐", rec, request);
        ledger.appendTimeline(run, AutonomyTimelinePhase.ACCEPTED.code(), message, recommendationId);
        ledger.update(run);

        logger.info("Recommendation accepted run={} id={} operator={}",
                run.getRunId(), recommendationId, rec.getOperator());
        return AdoptionResult.ok(run.getRunId(), recommendationId, RecommendationStatus.ACCEPTED, message);
    }

    /**
     * 拒绝配置级推荐。
     */
    public AdoptionResult rejectRecommendation(String recommendationId, AdoptionRequest request) {
        if (recommendationId == null || recommendationId.isBlank()) {
            return AdoptionResult.fail("INVALID", "recommendationId 不能为空");
        }
        Optional<ResolvedTarget> target = resolveRecommendation(recommendationId, request);
        if (target.isEmpty()) {
            return AdoptionResult.fail("NOT_FOUND", "未找到推荐 " + recommendationId);
        }
        AutonomyRun run = target.get().run();
        AutonomyRecommendation rec = target.get().recommendation();

        if (rec.getStatus() == RecommendationStatus.REJECTED) {
            return AdoptionResult.idempotent(run.getRunId(), recommendationId,
                    RecommendationStatus.REJECTED, "推荐已拒绝，无需重复操作");
        }
        if (rec.getStatus() == RecommendationStatus.ACCEPTED) {
            return AdoptionResult.fail("CONFLICT", "该推荐已采纳，不可再拒绝");
        }

        rec.setStatus(RecommendationStatus.REJECTED);
        rec.setDecidedAt(Instant.now());
        rec.setOperator(resolveOperator(request));
        rec.setRejectReason(request != null ? request.getComment() : null);

        String message = buildRecommendationMessage("拒绝推荐", rec, request);
        ledger.appendTimeline(run, AutonomyTimelinePhase.ACCEPTED.code(), message, recommendationId);
        ledger.update(run);

        logger.info("Recommendation rejected run={} id={} operator={}",
                run.getRunId(), recommendationId, rec.getOperator());
        return AdoptionResult.ok(run.getRunId(), recommendationId, RecommendationStatus.REJECTED, message);
    }

    /**
     * 人工采纳并执行备选方案（plan 中 rank≥2 或 ADVISE 的门控动作）。
     *
     * @param runId run 标识
     * @param rank  动作排序位（与 PlannedAction.rank 一致）
     */
    public AdoptionResult acceptAdvisedAction(String runId, int rank, AdoptionRequest request) {
        if (runId == null || runId.isBlank()) {
            return AdoptionResult.fail("INVALID", "runId 不能为空");
        }
        if (rank < 1) {
            return AdoptionResult.fail("INVALID", "rank 必须 ≥ 1");
        }
        Optional<AutonomyRun> runOpt = ledger.get(runId);
        if (runOpt.isEmpty()) {
            return AdoptionResult.fail("NOT_FOUND", "未找到 run " + runId);
        }
        AutonomyRun run = runOpt.get();
        if (run.getPlan() == null || run.getPlan().getActions() == null) {
            return AdoptionResult.fail("NOT_FOUND", "该 run 无计划动作");
        }

        PlannedAction action = run.getPlan().getActions().stream()
                .filter(a -> a.getRank() == rank)
                .findFirst()
                .orElse(null);
        if (action == null) {
            return AdoptionResult.fail("NOT_FOUND", "未找到 rank=" + rank + " 的动作");
        }
        if (action.isHumanAccepted()) {
            return AdoptionResult.actionOk(runId, rank,
                    "动作 " + action.getActionType() + " 已人工采纳执行，无需重复");
        }
        if (rank == 1 && action.getPolicyDecision() == AutonomyPolicyDecision.AUTO
                && "SUCCESS".equals(action.getExecutionStatus())) {
            return AdoptionResult.fail("CONFLICT", "该动作已自动执行成功，无需人工采纳");
        }

        action.setHumanAccepted(true);
        action.setPolicyDecision(AutonomyPolicyDecision.AUTO);

        String operator = resolveOperator(request);
        String acceptMsg = String.format("人工采纳执行 #%d %s（操作人 %s）",
                rank, action.getActionType(), operator);
        if (request != null && request.getComment() != null && !request.getComment().isBlank()) {
            acceptMsg += " — " + request.getComment();
        }
        ledger.appendTimeline(run, AutonomyTimelinePhase.ACCEPTED.code(), acceptMsg, null);

        actuator.execute(action);
        ledger.appendTimeline(run, AutonomyTimelinePhase.AUTO.code(),
                action.getActionType() + " → " + action.getExecutionStatus()
                        + ": " + action.getExecutionDetail(), null);
        ledger.update(run);

        logger.info("Advised action accepted run={} rank={} type={} status={}",
                runId, rank, action.getActionType(), action.getExecutionStatus());
        return AdoptionResult.actionOk(runId, rank, acceptMsg);
    }

    private Optional<ResolvedTarget> resolveRecommendation(String recommendationId, AdoptionRequest request) {
        if (request != null && request.getRunId() != null && !request.getRunId().isBlank()) {
            return ledger.get(request.getRunId())
                    .flatMap(run -> findRecommendation(run, recommendationId)
                            .map(rec -> new ResolvedTarget(run, rec)));
        }
        for (AutonomyRun run : ledger.listRecent(200)) {
            Optional<AutonomyRecommendation> rec = findRecommendation(run, recommendationId);
            if (rec.isPresent()) {
                return Optional.of(new ResolvedTarget(run, rec.get()));
            }
        }
        return Optional.empty();
    }

    private Optional<AutonomyRecommendation> findRecommendation(AutonomyRun run, String recommendationId) {
        if (run.getPlan() == null || run.getPlan().getRecommendations() == null) {
            return Optional.empty();
        }
        return run.getPlan().getRecommendations().stream()
                .filter(r -> recommendationId.equals(r.getRecommendationId()))
                .findFirst();
    }

    private String buildRecommendationMessage(String verb, AutonomyRecommendation rec, AdoptionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(verb).append("「").append(rec.getTitle()).append("」");
        if (rec.getSuggestedConfig() != null && !rec.getSuggestedConfig().isBlank()) {
            sb.append("，配置项 ").append(rec.getSuggestedConfig());
        }
        String operator = resolveOperator(request);
        sb.append("（操作人 ").append(operator).append("）");
        if (request != null && request.getComment() != null && !request.getComment().isBlank()) {
            sb.append(" — ").append(request.getComment());
        }
        return sb.toString();
    }

    private String resolveOperator(AdoptionRequest request) {
        if (request != null && request.getOperator() != null && !request.getOperator().isBlank()) {
            return request.getOperator();
        }
        return "console";
    }

    private record ResolvedTarget(AutonomyRun run, AutonomyRecommendation recommendation) {
    }
}
