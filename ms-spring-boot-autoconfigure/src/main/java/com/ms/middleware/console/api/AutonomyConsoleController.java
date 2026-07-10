package com.ms.middleware.console.api;

import com.ms.middleware.autonomy.insight.MiddlewareInsightService;
import com.ms.middleware.autonomy.run.AutonomyRun;
import com.ms.middleware.console.chat.ConsoleChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${ms.middleware.console.base-path:/ms-console}/api")
public class AutonomyConsoleController {

    private final MiddlewareInsightService insightService;
    private final ConsoleChatService chatService;

    public AutonomyConsoleController(MiddlewareInsightService insightService,
                                     ConsoleChatService chatService) {
        this.insightService = insightService;
        this.chatService = chatService;
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
        return body;
    }

    @PostMapping("/chat")
    public ConsoleChatResponse chat(@RequestBody ConsoleChatRequest request) {
        return chatService.chat(request.getMessage(), request.getRunId());
    }
}
