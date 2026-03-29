package com.ms.middleware.mq;

import java.time.Duration;

/**
 * 消息监听器配置
 * 用于配置消息监听器的属性
 */
public class ListenerConfig {

    /**
     * 并发消费者数量
     */
    private int concurrency = 1;

    /**
     * 是否自动确认
     */
    private boolean autoAck = true;

    /**
     * 重试次数
     */
    private int maxRetries = 3;

    /**
     * 重试间隔
     */
    private Duration retryInterval = Duration.ofSeconds(1);

    /**
     * 是否启用幂等消费
     */
    private boolean idempotent = true;

    /**
     * 幂等键前缀
     */
    private String idempotentPrefix = "mq:idempotent:";

    /**
     * 消息超时时间
     */
    private Duration timeout = Duration.ofMinutes(1);

    /**
     * 是否启用死信队列
     */
    private boolean deadLetterEnabled = false;

    /**
     * 死信交换机
     */
    private String deadLetterExchange = "dead-letter-exchange";

    /**
     * 死信路由键
     */
    private String deadLetterRoutingKey = "dead-letter";

    // Getters and Setters
    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public boolean isAutoAck() {
        return autoAck;
    }

    public void setAutoAck(boolean autoAck) {
        this.autoAck = autoAck;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Duration retryInterval) {
        this.retryInterval = retryInterval;
    }

    public boolean isIdempotent() {
        return idempotent;
    }

    public void setIdempotent(boolean idempotent) {
        this.idempotent = idempotent;
    }

    public String getIdempotentPrefix() {
        return idempotentPrefix;
    }

    public void setIdempotentPrefix(String idempotentPrefix) {
        this.idempotentPrefix = idempotentPrefix;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public boolean isDeadLetterEnabled() {
        return deadLetterEnabled;
    }

    public void setDeadLetterEnabled(boolean deadLetterEnabled) {
        this.deadLetterEnabled = deadLetterEnabled;
    }

    public String getDeadLetterExchange() {
        return deadLetterExchange;
    }

    public void setDeadLetterExchange(String deadLetterExchange) {
        this.deadLetterExchange = deadLetterExchange;
    }

    public String getDeadLetterRoutingKey() {
        return deadLetterRoutingKey;
    }

    public void setDeadLetterRoutingKey(String deadLetterRoutingKey) {
        this.deadLetterRoutingKey = deadLetterRoutingKey;
    }

    /**
     * 创建默认配置
     */
    public static ListenerConfig defaultConfig() {
        return new ListenerConfig();
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private final ListenerConfig config = new ListenerConfig();

        public Builder concurrency(int concurrency) {
            config.setConcurrency(concurrency);
            return this;
        }

        public Builder autoAck(boolean autoAck) {
            config.setAutoAck(autoAck);
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            config.setMaxRetries(maxRetries);
            return this;
        }

        public Builder retryInterval(Duration retryInterval) {
            config.setRetryInterval(retryInterval);
            return this;
        }

        public Builder idempotent(boolean idempotent) {
            config.setIdempotent(idempotent);
            return this;
        }

        public Builder idempotentPrefix(String idempotentPrefix) {
            config.setIdempotentPrefix(idempotentPrefix);
            return this;
        }

        public Builder timeout(Duration timeout) {
            config.setTimeout(timeout);
            return this;
        }

        public Builder deadLetterEnabled(boolean deadLetterEnabled) {
            config.setDeadLetterEnabled(deadLetterEnabled);
            return this;
        }

        public Builder deadLetterExchange(String deadLetterExchange) {
            config.setDeadLetterExchange(deadLetterExchange);
            return this;
        }

        public Builder deadLetterRoutingKey(String deadLetterRoutingKey) {
            config.setDeadLetterRoutingKey(deadLetterRoutingKey);
            return this;
        }

        public ListenerConfig build() {
            return config;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
