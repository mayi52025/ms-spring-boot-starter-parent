package com.ms.middleware.mq.idempotent;

import com.ms.middleware.mq.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 幂等消息消费者
 * 确保消息只被处理一次
 */
public class IdempotentConsumer {

    private static final Logger logger = LoggerFactory.getLogger(IdempotentConsumer.class);

    private final IdempotentStore idempotentStore;
    private final String prefix;
    private final long expiration;

    /**
     * 构造函数
     * @param idempotentStore 幂等存储
     * @param prefix 键前缀
     * @param expiration 过期时间（毫秒）
     */
    public IdempotentConsumer(IdempotentStore idempotentStore, String prefix, long expiration) {
        this.idempotentStore = idempotentStore;
        this.prefix = prefix;
        this.expiration = expiration;
    }

    /**
     * 构造函数
     * @param idempotentStore 幂等存储
     * @param prefix 键前缀
     */
    public IdempotentConsumer(IdempotentStore idempotentStore, String prefix) {
        this(idempotentStore, prefix, TimeUnit.HOURS.toMillis(24));
    }

    /**
     * 处理消息
     * @param messageId 消息ID
     * @param message 消息内容
     * @param headers 消息属性
     * @param listener 消息监听器
     * @return 处理结果
     */
    public boolean process(String messageId, Object message, Map<String, Object> headers, MessageListener listener) {
        if (messageId == null) {
            logger.warn("Message ID is null, skip idempotent check");
            return listener.onMessage(message, headers, messageId);
        }

        String key = prefix + messageId;

        try {
            // 尝试获取锁
            if (idempotentStore.acquire(key, expiration)) {
                try {
                    // 处理消息
                    return listener.onMessage(message, headers, messageId);
                } finally {
                    // 释放锁
                    idempotentStore.release(key);
                }
            } else {
                logger.info("Message {} has been processed before, skip", messageId);
                return true; // 已经处理过，返回成功
            }
        } catch (Exception e) {
            logger.error("Idempotent processing failed", e);
            return false;
        }
    }

    /**
     * 处理消息
     * @param messageId 消息ID
     * @param message 消息内容
     * @param listener 消息监听器
     * @return 处理结果
     */
    public boolean process(String messageId, Object message, MessageListener listener) {
        return process(messageId, message, null, listener);
    }
}
