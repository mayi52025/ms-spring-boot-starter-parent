package com.ms.middleware.autonomy.insight;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * 失败 MQ 消息 Trace 的 API 视图（Phase 4 Step 2）。
 *
 * <p>供控制台列表与复制 messageId，避免暴露完整 {@link com.ms.middleware.mq.trace.MessageTrace} 内部字段。</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FailedMessageTraceView {

    private String messageId;
    private String queue;
    private String errorMessage;
    private Long processTimeMs;
    private Instant processTime;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getProcessTimeMs() {
        return processTimeMs;
    }

    public void setProcessTimeMs(Long processTimeMs) {
        this.processTimeMs = processTimeMs;
    }

    public Instant getProcessTime() {
        return processTime;
    }

    public void setProcessTime(Instant processTime) {
        this.processTime = processTime;
    }
}
