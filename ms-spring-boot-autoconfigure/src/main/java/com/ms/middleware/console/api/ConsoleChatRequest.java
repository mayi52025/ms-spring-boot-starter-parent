package com.ms.middleware.console.api;

public class ConsoleChatRequest {

    private String message;
    private String runId;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }
}
