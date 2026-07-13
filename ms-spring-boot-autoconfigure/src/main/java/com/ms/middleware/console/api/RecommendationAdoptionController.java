package com.ms.middleware.console.api;

import com.ms.middleware.autonomy.adoption.AdoptionRequest;
import com.ms.middleware.autonomy.adoption.AdoptionResult;
import com.ms.middleware.autonomy.adoption.HumanAdoptionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人机协同采纳 API：配置推荐与备选 ADVISE 动作的审计与执行。
 *
 * <p>路径前缀与 {@link AutonomyConsoleController} 一致，默认 {@code /ms-console/api}。</p>
 */
@RestController
@RequestMapping("${ms.middleware.console.base-path:/ms-console}/api")
@ConditionalOnBean(HumanAdoptionService.class)
public class RecommendationAdoptionController {

    private final HumanAdoptionService adoptionService;

    public RecommendationAdoptionController(HumanAdoptionService adoptionService) {
        this.adoptionService = adoptionService;
    }

    /**
     * 采纳配置级推荐。
     * 幂等：已采纳返回 200 + ALREADY_ACCEPTED。
     */
    @PostMapping("/recommendations/{recommendationId}/accept")
    public ResponseEntity<AdoptionResult> acceptRecommendation(
            @PathVariable String recommendationId,
            @RequestBody(required = false) AdoptionRequest request) {
        AdoptionResult result = adoptionService.acceptRecommendation(recommendationId, request);
        return toResponse(result);
    }

    /**
     * 拒绝配置级推荐。
     */
    @PostMapping("/recommendations/{recommendationId}/reject")
    public ResponseEntity<AdoptionResult> rejectRecommendation(
            @PathVariable String recommendationId,
            @RequestBody(required = false) AdoptionRequest request) {
        AdoptionResult result = adoptionService.rejectRecommendation(recommendationId, request);
        return toResponse(result);
    }

    /**
     * 人工采纳并执行备选方案（rank≥2 或 ADVISE 门控动作）。
     */
    @PostMapping("/runs/{runId}/actions/{rank}/accept")
    public ResponseEntity<AdoptionResult> acceptAdvisedAction(
            @PathVariable String runId,
            @PathVariable int rank,
            @RequestBody(required = false) AdoptionRequest request) {
        AdoptionResult result = adoptionService.acceptAdvisedAction(runId, rank, request);
        return toResponse(result);
    }

    private ResponseEntity<AdoptionResult> toResponse(AdoptionResult result) {
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        HttpStatus status = switch (result.getCode()) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "CONFLICT" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(result);
    }
}
