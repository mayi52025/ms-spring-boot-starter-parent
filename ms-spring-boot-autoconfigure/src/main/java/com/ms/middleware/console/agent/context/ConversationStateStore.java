package com.ms.middleware.console.agent.context;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话状态存储：按 sessionId + runId 分桶，换 run 即新线程（与前端清聊天配合）。
 */
@Component
public class ConversationStateStore {

    private final ConcurrentHashMap<String, ConversationState> states = new ConcurrentHashMap<>();

    public ConversationState getOrCreate(String sessionId, String runId) {
        String key = buildKey(sessionId, runId);
        return states.computeIfAbsent(key, ignored -> new ConversationState());
    }

    public void remove(String sessionId, String runId) {
        states.remove(buildKey(sessionId, runId));
    }

    static String buildKey(String sessionId, String runId) {
        String sid = sessionId != null && !sessionId.isBlank() ? sessionId.trim() : "_anonymous_";
        String rid = runId != null && !runId.isBlank() ? runId.trim() : "_global_";
        return sid + "::" + rid;
    }
}
