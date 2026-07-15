package com.ms.middleware.console.api;

import java.util.ArrayList;
import java.util.List;

public class ConsoleChatResponse {

    private String reply;
    private boolean llmEnabled;
    private List<String> toolsUsed = List.of();
    private boolean grounded;
    /** 5.3 工作上下文注入层 hint，如 runSnapshot / dialogState */
    private List<String> contextHints = List.of();
    /** 实际绑定的 runId（含自动绑定单活跃故障） */
    private String boundRunId;

    public ConsoleChatResponse() {
    }

    public ConsoleChatResponse(String reply, boolean llmEnabled) {
        this(reply, llmEnabled, List.of(), true, List.of(), null);
    }

    public ConsoleChatResponse(String reply, boolean llmEnabled, List<String> toolsUsed, boolean grounded) {
        this(reply, llmEnabled, toolsUsed, grounded, List.of(), null);
    }

    public ConsoleChatResponse(String reply,
                               boolean llmEnabled,
                               List<String> toolsUsed,
                               boolean grounded,
                               List<String> contextHints,
                               String boundRunId) {
        this.reply = reply;
        this.llmEnabled = llmEnabled;
        setToolsUsed(toolsUsed);
        this.grounded = grounded;
        setContextHints(contextHints);
        this.boundRunId = boundRunId;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public boolean isLlmEnabled() {
        return llmEnabled;
    }

    public void setLlmEnabled(boolean llmEnabled) {
        this.llmEnabled = llmEnabled;
    }

    public List<String> getToolsUsed() {
        return toolsUsed;
    }

    public void setToolsUsed(List<String> toolsUsed) {
        if (toolsUsed == null || toolsUsed.isEmpty()) {
            this.toolsUsed = List.of();
        } else {
            this.toolsUsed = List.copyOf(new ArrayList<>(toolsUsed));
        }
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public List<String> getContextHints() {
        return contextHints;
    }

    public void setContextHints(List<String> contextHints) {
        if (contextHints == null || contextHints.isEmpty()) {
            this.contextHints = List.of();
        } else {
            this.contextHints = List.copyOf(new ArrayList<>(contextHints));
        }
    }

    public String getBoundRunId() {
        return boundRunId;
    }

    public void setBoundRunId(String boundRunId) {
        this.boundRunId = boundRunId;
    }
}
