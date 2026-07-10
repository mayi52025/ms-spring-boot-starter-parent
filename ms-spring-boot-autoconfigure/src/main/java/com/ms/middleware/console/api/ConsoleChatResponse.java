package com.ms.middleware.console.api;

public class ConsoleChatResponse {

    private String reply;
    private boolean llmEnabled;

    public ConsoleChatResponse(String reply, boolean llmEnabled) {
        this.reply = reply;
        this.llmEnabled = llmEnabled;
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
}
