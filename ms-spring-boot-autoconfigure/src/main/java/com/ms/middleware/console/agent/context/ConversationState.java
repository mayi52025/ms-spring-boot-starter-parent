package com.ms.middleware.console.agent.context;

import com.ms.middleware.console.agent.grounding.GroundingIntent;

import java.util.ArrayList;
import java.util.List;

/**
 * 压缩对话态：存结构化状态，而非完整 assistant 回复（省 token、防串台）。
 */
public class ConversationState {

    private String boundRunId;
    private GroundingIntent lastIntent;
    private List<String> lastToolsUsed = new ArrayList<>();
    private final List<String> recentUserMessages = new ArrayList<>();

    public String getBoundRunId() {
        return boundRunId;
    }

    public void setBoundRunId(String boundRunId) {
        this.boundRunId = boundRunId;
    }

    public GroundingIntent getLastIntent() {
        return lastIntent;
    }

    public void setLastIntent(GroundingIntent lastIntent) {
        this.lastIntent = lastIntent;
    }

    public List<String> getLastToolsUsed() {
        return lastToolsUsed;
    }

    public void setLastToolsUsed(List<String> lastToolsUsed) {
        this.lastToolsUsed = lastToolsUsed != null ? new ArrayList<>(lastToolsUsed) : new ArrayList<>();
    }

    public List<String> getRecentUserMessages() {
        return recentUserMessages;
    }

    /** 追加用户原话，保留最近 max 条 */
    public void appendUserMessage(String message, int maxMessages) {
        if (message == null || message.isBlank()) {
            return;
        }
        recentUserMessages.add(message.trim());
        while (recentUserMessages.size() > Math.max(1, maxMessages)) {
            recentUserMessages.remove(0);
        }
    }
}
