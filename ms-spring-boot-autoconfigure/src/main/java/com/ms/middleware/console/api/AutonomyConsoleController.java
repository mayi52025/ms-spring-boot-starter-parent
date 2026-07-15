package com.ms.middleware.console.api;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.insight.FailedMessageTraceView;
import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.console.auth.ConsoleAuthSupport;
import com.ms.middleware.console.chat.ConsoleChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制台 REST API：run 列表、活跃故障、指标快照、规则聊天。
 * 路径前缀由 {@code ms.middleware.console.base-path} 控制，默认 /ms-console/api。
 */
@RestController
@RequestMapping("${ms.middleware.console.base-path:/ms-console}/api")
public class AutonomyConsoleController {

    private final MiddlewareInsightService insightService;
    private final ConsoleChatService chatService;
    private final MsMiddlewareProperties properties;
    private final ConsoleAuthSupport consoleAuthSupport;

    public AutonomyConsoleController(MiddlewareInsightService insightService,
                                     ConsoleChatService chatService,
                                     MsMiddlewareProperties properties,
                                     ConsoleAuthSupport consoleAuthSupport) {
        this.insightService = insightService;
        this.chatService = chatService;
        this.properties = properties;
        this.consoleAuthSupport = consoleAuthSupport;
    }

    /** 鉴权状态与控制台能力探测（无需 token），供静态页决定是否弹出登录 */
    @GetMapping("/auth/status")
    public Map<String, Object> authStatus() {
        Map<String, Object> body = new HashMap<>();
        body.put("authRequired", consoleAuthSupport.isAuthRequired(properties.getConsole()));
        body.put("adoptionMode", properties.getAutonomy().getAdoption().getMode());
        body.put("llmEnabled", properties.getConsole().isLlmEnabled());
        return body;
    }

    @GetMapping("/runs")
    public List<AutonomyRun> listRuns() {
        return insightService.listRecentRuns(200);
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<AutonomyRun> getRun(@PathVariable String runId) {
        return insightService.getRun(runId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/issues")
    public Map<String, Object> activeIssues() {
        List<AutonomyRun> active = insightService.listActiveRuns();
        Map<String, Object> body = new HashMap<>();
        body.put("count", active.size());
        body.put("runs", active);
        body.put("healthy", active.isEmpty());
        return body;
    }

    @GetMapping("/history")
    public Map<String, Object> historyRuns() {
        List<AutonomyRun> history = insightService.listHistoryRuns(50);
        Map<String, Object> body = new HashMap<>();
        body.put("count", history.size());
        body.put("runs", history);
        return body;
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        var snapshot = insightService.getMetrics();
        Map<String, Object> body = new HashMap<>();
        body.put("cacheHitRate", snapshot.getCacheHitRate());
        body.put("mqFailedCount", snapshot.getMqFailedCount());
        body.put("globalFailureCount", snapshot.getGlobalFailureCount());
        body.put("activeRunCount", snapshot.getActiveRunCount());
        body.put("lastMttrSeconds", snapshot.getLastMttrSeconds());
        body.put("completedAutonomyRuns", snapshot.getCompletedAutonomyRuns());
        return body;
    }

    /**
     * 近期消费失败的 MQ trace 列表，供控制台复制 messageId 排障。
     */
    @GetMapping("/traces/failed")
    public Map<String, Object> failedTraces(@RequestParam(defaultValue = "20") int limit) {
        List<FailedMessageTraceView> traces = insightService.listFailedTraces(limit);
        Map<String, Object> body = new HashMap<>();
        body.put("count", traces.size());
        body.put("traces", traces);
        return body;
    }

    @PostMapping("/chat")
    public ConsoleChatResponse chat(@RequestBody ConsoleChatRequest request) {
        return chatService.chat(request.getMessage(), request.getRunId());
    }
}
