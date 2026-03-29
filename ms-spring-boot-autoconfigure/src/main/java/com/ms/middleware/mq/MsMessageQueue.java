package com.ms.middleware.mq;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 消息队列接口
 * 定义消息队列的核心操作
 */
public interface MsMessageQueue {

    /**
     * 发送消息
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息内容
     * @return 消息发送结果
     */
    boolean send(String exchange, String routingKey, Object message);

    /**
     * 发送消息（带回调）
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息内容
     * @return 异步结果
     */
    CompletableFuture<Boolean> sendAsync(String exchange, String routingKey, Object message);

    /**
     * 发送消息（带属性）
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息内容
     * @param headers 消息属性
     * @return 消息发送结果
     */
    boolean sendWithHeaders(String exchange, String routingKey, Object message, Map<String, Object> headers);

    /**
     * 发送延迟消息
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息内容
     * @param delay 延迟时间（毫秒）
     * @return 消息发送结果
     */
    boolean sendDelayed(String exchange, String routingKey, Object message, long delay);

    /**
     * 发送顺序消息
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息内容
     * @param orderKey 顺序键
     * @return 消息发送结果
     */
    boolean sendOrdered(String exchange, String routingKey, Object message, String orderKey);

    /**
     * 注册消息监听器
     * @param queueName 队列名称
     * @param listener 消息监听器
     */
    void registerListener(String queueName, MessageListener listener);

    /**
     * 注册消息监听器（带配置）
     * @param queueName 队列名称
     * @param listener 消息监听器
     * @param config 监听器配置
     */
    void registerListener(String queueName, MessageListener listener, ListenerConfig config);

    /**
     * 关闭消息队列
     */
    void close();
}
