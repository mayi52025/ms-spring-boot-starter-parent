package com.ms.middleware.console.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.autonomy.run.ConsoleTimelineEvent;
import com.ms.middleware.autonomy.run.TimelineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConsoleStreamHub {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleStreamHub.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public ConsoleStreamHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe(long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            logger.warn("SSE connect failed", e);
        }
        return emitter;
    }

    @EventListener
    public void onTimeline(ConsoleTimelineEvent event) {
        broadcast(event.getTimelineEvent());
    }

    public void broadcast(TimelineEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            payload = event.getMessage();
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("timeline").data(payload));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}
