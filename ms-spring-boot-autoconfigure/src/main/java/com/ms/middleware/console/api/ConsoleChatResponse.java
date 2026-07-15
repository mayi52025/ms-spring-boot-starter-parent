package com.ms.middleware.console.api;

import java.util.ArrayList;
import java.util.List;

public class ConsoleChatResponse {

    private String reply;
    private boolean llmEnabled;
    private List<String> toolsUsed = List.of();
    private boolean grounded;

    public ConsoleChatResponse() {
    }

    public ConsoleChatResponse(String reply, boolean llmEnabled) {
        this(reply, llmEnabled, List.of(), true);
    }

    public ConsoleChatResponse(String reply, boolean llmEnabled, List<String> toolsUsed, boolean grounded) {
        this.reply = reply;
        this.llmEnabled = llmEnabled;
        setToolsUsed(toolsUsed);
        this.grounded = grounded;
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
}
