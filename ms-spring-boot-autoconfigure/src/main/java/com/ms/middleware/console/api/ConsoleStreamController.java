package com.ms.middleware.console.api;

import com.ms.middleware.console.stream.ConsoleStreamHub;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** SSE 端点：浏览器 EventSource 连接 {@code GET /ms-console/api/stream} */
@RestController
@RequestMapping("${ms.middleware.console.base-path:/ms-console}/api")
public class ConsoleStreamController {

    private final ConsoleStreamHub streamHub;

    public ConsoleStreamController(ConsoleStreamHub streamHub) {
        this.streamHub = streamHub;
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        return streamHub.subscribe(0L);
    }
}
