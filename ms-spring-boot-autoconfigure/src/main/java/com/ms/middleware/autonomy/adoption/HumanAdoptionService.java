package com.ms.middleware.autonomy.adoption;

import com.ms.middleware.autonomy.AutonomyPolicyDecision;
import com.ms.middleware.autonomy.act.AutonomyActuator;
import com.ms.middleware.autonomy.adoption.nacos.NacosConfigDraft;
import com.ms.middleware.autonomy.adoption.nacos.NacosConfigDraftService;
import com.ms.middleware.autonomy.adoption.nacos.NacosDraftRequest;
import com.ms.middleware.autonomy.plan.AutonomyRecommendation;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.autonomy.plan.RecommendationStatus;
import com.ms.middleware.autonomy.metrics.AutonomyMetrics;
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
 *   <li>{@code audit-only}（默认）仅记审计，不自动改 Nacos</li>
 *   <li>{@code nacos-draft} 采纳时生成 draft + diff，二次「确认发布」才写入生产 dataId</li>
 *   <li>备选动作采纳后走 {@link AutonomyActuator} 真正执行，并补 AUTO 时间线</li>
 * </ul>
 */
public class HumanAdoptionService {

    private static final Logger logger = LoggerFactory.getLogger(HumanAdoptionService.class);

    private final AutonomyLedger ledger;
    private final AutonomyActuator actuator;
    private final AutonomyMetrics autonomyMetrics;
    private final NacosConfigDraftService draftService;

    public HumanAdoptionService(AutonomyLedger ledger,
                                AutonomyActuator actuator,
                                AutonomyMetrics autonomyMetrics,
                                NacosConfigDraftService draftService) {
        this.ledger = ledger;
        this.actuator = actuator;
        this.autonomyMetrics = autonomyMetrics;
        this.draftService = draftService != null ? draftService : NacosConfigDraftService.noop();
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
            AdoptionResult idempotent = AdoptionResult.idempotent(run.getRunId(), recommendationId,
                    RecommendationStatus.ACCEPTED, "推荐已采纳，无需重复操作");
            enrichDraftFields(idempotent, rec);
            return idempotent;
        }
        if (rec.getStatus() == RecommendationStatus.REJECTED) {
            return AdoptionResult.fail("CONFLICT", "该推荐已拒绝，不可再采纳");
        }

        NacosConfigDraft draft = null;
        if (draftService.isDraftModeEnabled() && hasSuggestedConfig(rec)) {
            Optional<NacosConfigDraft> draftOpt = draftService.createDraft(buildDraftRequest(run, rec, request));
            if (draftOpt.isEmpty()) {
                return AdoptionResult.fail("DRAFT_FAILED", "Nacos 配置草稿创建失败，请检查 Nacos 连接或 suggestedConfig");
            }
            draft = draftOpt.get();
            rec.setDraftId(draft.getDraftId());
            rec.setDiffSummary(draft.getDiffSummary());
            rec.setNacosPublished(false);
        }

        rec.setStatus(RecommendationStatus.ACCEPTED);
        rec.setDecidedAt(Instant.now());
        rec.setOperator(resolveOperator(request));
        rec.setRejectReason(null);

        String message = buildRecommendationMessage("采纳推荐", rec, request, draft);
        ledger.appendTimeline(run, AutonomyTimelinePhase.ACCEPTED.code(), message, recommendationId);
        ledger.update(run);

        autonomyMetrics.recordRecommendationAccepted(run.getTenant(), incidentTypeOf(run));

        logger.info("Recommendation accepted run={} id={} operator={} draftMode={}",
                run.getRunId(), recommendationId, rec.getOperator(), draftService.isDraftModeEnabled());
        AdoptionResult result = AdoptionResult.ok(run.getRunId(), recommendationId, RecommendationStatus.ACCEPTED, message);
        if (draft != null) {
            AdoptionResult.withDraft(result, draft.getDraftId(), draft.getDiffSummary());
        }
        return result;
    }

    /**
     * nacos-draft 模式：二次确认，将草稿 publish 到生产 dataId。
     */
    public AdoptionResult publishRecommendationDraft(String recommendationId, AdoptionRequest request) {
        if (recommendationId == null || recommendationId.isBlank()) {
            return AdoptionResult.fail("INVALID", "recommendationId 不能为空");
        }
        if (!draftService.isDraftModeEnabled()) {
            return AdoptionResult.fail("NOT_SUPPORTED", "当前为 audit-only 模式，不支持 Nacos 发布");
        }
        Optional<ResolvedTarget> target = resolveRecommendation(recommendationId, request);
        if (target.isEmpty()) {
            return AdoptionResult.fail("NOT_FOUND", "未找到推荐 " + recommendationId);
        }
        AutonomyRun run = target.get().run();
        AutonomyRecommendation rec = target.get().recommendation();

        if (rec.getStatus() != RecommendationStatus.ACCEPTED) {
            return AdoptionResult.fail("CONFLICT", "须先采纳推荐，再确认发布 Nacos 配置");
        }
        if (rec.isNacosPublished()) {
            return AdoptionResult.alreadyPublished(run.getRunId(), recommendationId,
                    rec.getDraftId(), "配置已发布到生产，无需重复操作");
        }

        String draftId = rec.getDraftId() != null && !rec.getDraftId().isBlank()
                ? rec.getDraftId() : recommendationId;
        Optional<NacosConfigDraft> published = draftService.publishDraft(draftId);
        if (published.isEmpty()) {
            return AdoptionResult.fail("PUBLISH_FAILED", "Nacos 配置发布失败，draftId=" + draftId);
        }

        NacosConfigDraft draft = published.get();
        rec.setNacosPublished(true);
        rec.setDraftId(draft.getDraftId());

        String operator = resolveOperator(request);
        String message = String.format("确认发布 Nacos 配置 draftId=%s → %s（操作人 %s）",
                draftId, draft.getProductionDataId(), operator);
        if (request != null && request.getComment() != null && !request.getComment().isBlank()) {
            message += " — " + request.getComment();
        }
        ledger.appendTimeline(run, AutonomyTimelinePhase.PUBLISH.code(), message, recommendationId);
        ledger.update(run);

        logger.info("Nacos draft published run={} id={} productionDataId={}",
                run.getRunId(), recommendationId, draft.getProductionDataId());
        return AdoptionResult.publishOk(run.getRunId(), recommendationId, draftId, rec.getDiffSummary(), message);
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

        String message = buildRecommendationMessage("拒绝推荐", rec, request, null);
        ledger.appendTimeline(run, AutonomyTimelinePhase.ACCEPTED.code(), message, recommendationId);
        ledger.update(run);

        autonomyMetrics.recordRecommendationRejected(run.getTenant(), incidentTypeOf(run));

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

        autonomyMetrics.recordActionAuto(
                run.getTenant(),
                incidentTypeOf(run),
                action.getActionType().name(),
                "human");

        logger.info("Advised action accepted run={} rank={} type={} status={}",
                runId, rank, action.getActionType(), action.getExecutionStatus());
        return AdoptionResult.actionOk(runId, rank, acceptMsg);
    }

    private NacosDraftRequest buildDraftRequest(AutonomyRun run, AutonomyRecommendation rec, AdoptionRequest request) {
        NacosDraftRequest draftRequest = new NacosDraftRequest();
        draftRequest.setApplicationName(run.getTenant());
        draftRequest.setRecommendationId(rec.getRecommendationId());
        draftRequest.setSuggestedConfig(rec.getSuggestedConfig());
        draftRequest.setOperator(resolveOperator(request));
        return draftRequest;
    }

    private static boolean hasSuggestedConfig(AutonomyRecommendation rec) {
        return rec.getSuggestedConfig() != null && !rec.getSuggestedConfig().isBlank();
    }

    private static void enrichDraftFields(AdoptionResult result, AutonomyRecommendation rec) {
        if (rec.getDraftId() != null) {
            result.setDraftId(rec.getDraftId());
            result.setDiffSummary(rec.getDiffSummary());
            result.setNacosPublished(rec.isNacosPublished());
        }
    }

    private Optional<ResolvedTarget> resolveRecommendation(String recommendationId, AdoptionRequest request) {
        if (request != null && request.getRunId() != null && !request.getRunId().isBlank()) {
            // ledger.get 已按当前 tenant 隔离，无法跨应用采纳
            return ledger.get(request.getRunId())
                    .flatMap(run -> findRecommendation(run, recommendationId)
                            .map(rec -> new ResolvedTarget(run, rec)));
        }
        // listRecent 仅返回当前 tenant 的 run
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

    private String buildRecommendationMessage(String verb, AutonomyRecommendation rec,
                                                AdoptionRequest request, NacosConfigDraft draft) {
        StringBuilder sb = new StringBuilder();
        sb.append(verb).append("「").append(rec.getTitle()).append("」");
        if ("采纳推荐".equals(verb)) {
            if (draft != null) {
                sb.append("，已生成 Nacos 草稿 draftId=").append(draft.getDraftId());
                if (draft.getDiffSummary() != null && !draft.getDiffSummary().isBlank()) {
                    sb.append("，diff: ").append(draft.getDiffSummary().replace("\n", " "));
                }
                sb.append("（待二次确认发布）");
            } else if (draftService.isDraftModeEnabled()) {
                sb.append("，无 suggestedConfig，未生成草稿");
            } else {
                sb.append("，未实际修改配置");
            }
        }
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

    private String incidentTypeOf(AutonomyRun run) {
        if (run.getPlan() != null && run.getPlan().getIncidentType() != null) {
            return run.getPlan().getIncidentType();
        }
        return "UNKNOWN";
    }

    private record ResolvedTarget(AutonomyRun run, AutonomyRecommendation recommendation) {
    }
}
