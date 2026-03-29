package com.ms.middleware.mq.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息追踪管理器
 * 用于管理和存储消息追踪信息
 */
public class MessageTraceManager {

    private static final Logger logger = LoggerFactory.getLogger(MessageTraceManager.class);

    /**
     * 消息追踪信息缓存
     */
    private final Map<String, MessageTrace> traceCache = new ConcurrentHashMap<>();

    /**
     * 最大缓存大小
     */
    private final int maxCacheSize;

    /**
     * 单例实例
     */
    private static volatile MessageTraceManager instance;

    /**
     * 私有构造函数
     */
    private MessageTraceManager(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * 获取单例实例
     */
    public static MessageTraceManager getInstance() {
        if (instance == null) {
            synchronized (MessageTraceManager.class) {
                if (instance == null) {
                    instance = new MessageTraceManager(10000);
                }
            }
        }
        return instance;
    }

    /**
     * 记录消息发送
     * @param trace 消息追踪信息
     */
    public void recordSend(MessageTrace trace) {
        if (trace == null || trace.getMessageId() == null) {
            return;
        }

        if (traceCache.size() >= maxCacheSize) {
            // 清理过期的追踪信息
            cleanExpiredTraces();
        }

        traceCache.put(trace.getMessageId(), trace);
        logger.debug("Message sent: {}", trace);
    }

    /**
     * 记录消息接收
     * @param messageId 消息ID
     * @param queue 队列名称
     * @param receiver 接收者
     */
    public void recordReceive(String messageId, String queue, String receiver) {
        MessageTrace trace = traceCache.get(messageId);
        if (trace != null) {
            trace.setQueue(queue);
            trace.setReceiveTime(java.time.Instant.now());
            trace.setReceiver(receiver);
            logger.debug("Message received: {}", trace);
        }
    }

    /**
     * 记录消息处理
     * @param messageId 消息ID
     * @param success 是否成功
     * @param errorMessage 错误信息
     * @param processTimeMs 处理耗时
     */
    public void recordProcess(String messageId, boolean success, String errorMessage, long processTimeMs) {
        MessageTrace trace = traceCache.get(messageId);
        if (trace != null) {
            trace.setProcessTime(java.time.Instant.now());
            trace.setSuccess(success);
            trace.setErrorMessage(errorMessage);
            trace.setProcessTimeMs(processTimeMs);
            logger.debug("Message processed: {}", trace);
        }
    }

    /**
     * 获取消息追踪信息
     * @param messageId 消息ID
     * @return 消息追踪信息
     */
    public MessageTrace getTrace(String messageId) {
        return traceCache.get(messageId);
    }

    /**
     * 清理过期的追踪信息
     */
    private void cleanExpiredTraces() {
        // 清理30分钟前的追踪信息
        long cutoffTime = System.currentTimeMillis() - (30 * 60 * 1000);
        traceCache.entrySet().removeIf(entry -> {
            MessageTrace trace = entry.getValue();
            return trace.getSendTime() != null && 
                   trace.getSendTime().toEpochMilli() < cutoffTime;
        });
    }

    /**
     * 清理所有追踪信息
     */
    public void clear() {
        traceCache.clear();
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return traceCache.size();
    }
}
