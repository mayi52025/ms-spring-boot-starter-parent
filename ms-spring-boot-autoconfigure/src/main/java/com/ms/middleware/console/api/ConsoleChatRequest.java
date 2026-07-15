package com.ms.middleware.console.api;

public class ConsoleChatRequest {

    private String message;
    /** 控制台选中的 runId（可选） */
    private String runId;
    /** 5.3 会话标识，前端 sessionStorage 持久化 */
    private String sessionId;

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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
