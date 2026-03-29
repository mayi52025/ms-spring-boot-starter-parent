package com.ms.middleware.mq.trace;

import java.time.Instant;
import java.util.Map;

/**
 * 消息追踪信息
 * 用于记录消息的全链路追踪信息
 */
public class MessageTrace {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 发送时间
     */
    private Instant sendTime;

    /**
     * 发送者
     */
    private String sender;

    /**
     * 交换机
     */
    private String exchange;

    /**
     * 路由键
     */
    private String routingKey;

    /**
     * 队列
     */
    private String queue;

    /**
     * 接收时间
     */
    private Instant receiveTime;

    /**
     * 接收者
     */
    private String receiver;

    /**
     * 处理时间
     */
    private Instant processTime;

    /**
     * 处理结果
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 处理耗时（毫秒）
     */
    private long processTimeMs;

    /**
     * 扩展属性
     */
    private Map<String, Object> extra;

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Instant getSendTime() {
        return sendTime;
    }

    public void setSendTime(Instant sendTime) {
        this.sendTime = sendTime;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public Instant getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(Instant receiveTime) {
        this.receiveTime = receiveTime;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public Instant getProcessTime() {
        return processTime;
    }

    public void setProcessTime(Instant processTime) {
        this.processTime = processTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getProcessTimeMs() {
        return processTimeMs;
    }

    public void setProcessTimeMs(long processTimeMs) {
        this.processTimeMs = processTimeMs;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    @Override
    public String toString() {
        return "MessageTrace{" +
                "messageId='" + messageId + '\'' +
                ", businessId='" + businessId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", sendTime=" + sendTime +
                ", sender='" + sender + '\'' +
                ", exchange='" + exchange + '\'' +
                ", routingKey='" + routingKey + '\'' +
                ", queue='" + queue + '\'' +
                ", receiveTime=" + receiveTime +
                ", receiver='" + receiver + '\'' +
                ", processTime=" + processTime +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", processTimeMs=" + processTimeMs +
                '}';
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private final MessageTrace trace = new MessageTrace();

        public Builder messageId(String messageId) {
            trace.setMessageId(messageId);
            return this;
        }

        public Builder businessId(String businessId) {
            trace.setBusinessId(businessId);
            return this;
        }

        public Builder messageType(String messageType) {
            trace.setMessageType(messageType);
            return this;
        }

        public Builder sendTime(Instant sendTime) {
            trace.setSendTime(sendTime);
            return this;
        }

        public Builder sender(String sender) {
            trace.setSender(sender);
            return this;
        }

        public Builder exchange(String exchange) {
            trace.setExchange(exchange);
            return this;
        }

        public Builder routingKey(String routingKey) {
            trace.setRoutingKey(routingKey);
            return this;
        }

        public Builder queue(String queue) {
            trace.setQueue(queue);
            return this;
        }

        public Builder receiveTime(Instant receiveTime) {
            trace.setReceiveTime(receiveTime);
            return this;
        }

        public Builder receiver(String receiver) {
            trace.setReceiver(receiver);
            return this;
        }

        public Builder processTime(Instant processTime) {
            trace.setProcessTime(processTime);
            return this;
        }

        public Builder success(boolean success) {
            trace.setSuccess(success);
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            trace.setErrorMessage(errorMessage);
            return this;
        }

        public Builder processTimeMs(long processTimeMs) {
            trace.setProcessTimeMs(processTimeMs);
            return this;
        }

        public Builder extra(Map<String, Object> extra) {
            trace.setExtra(extra);
            return this;
        }

        public MessageTrace build() {
            return trace;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
