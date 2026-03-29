package com.ms.middleware.mq;

import java.util.Map;

/**
 * 消息监听器接口
 * 处理接收到的消息
 */
@FunctionalInterface
public interface MessageListener {

    /**
     * 处理消息
     * @param message 消息内容
     * @param headers 消息属性
     * @param messageId 消息ID
     * @return 处理结果
     */
    boolean onMessage(Object message, Map<String, Object> headers, String messageId);

    /**
     * 处理消息（默认实现）
     * @param message 消息内容
     * @return 处理结果
     */
    default boolean onMessage(Object message) {
        return onMessage(message, null, null);
    }

    /**
     * 处理消息（带属性）
     * @param message 消息内容
     * @param headers 消息属性
     * @return 处理结果
     */
    default boolean onMessage(Object message, Map<String, Object> headers) {
        return onMessage(message, headers, null);
    }
}
