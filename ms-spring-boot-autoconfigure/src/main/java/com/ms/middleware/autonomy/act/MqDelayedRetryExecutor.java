package com.ms.middleware.autonomy.act;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.mq.MsMessageQueue;
import com.ms.middleware.mq.trace.MessageTrace;
import com.ms.middleware.mq.trace.MessageTraceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * MQ 失败消息批量延迟重试执行器。
 *
 * <p>对应自治动作 {@link com.ms.middleware.autonomy.AutonomyActionType#DELAYED_RETRY_BATCH}（MEDIUM 风险，通常 ADVISE）。
 * 从 {@link MessageTraceManager} 取出近期失败 trace，通过 {@link MsMessageQueue#sendDelayed} 重新投递。</p>
 */
public class MqDelayedRetryExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MqDelayedRetryExecutor.class);

    private static final String RETRY_PAYLOAD_KEY = "retryPayload";

    private final MsMessageQueue messageQueue;
    private final MessageTraceManager traceManager;
    private final MsMiddlewareProperties properties;

    public MqDelayedRetryExecutor(MsMessageQueue messageQueue,
                                MessageTraceManager traceManager,
                                MsMiddlewareProperties properties) {
        this.messageQueue = messageQueue;
        this.traceManager = traceManager;
        this.properties = properties;
    }

    /**
     * 批量延迟重试失败消息。
     *
     * @return 成功投递的重试消息数量
     */
    public int retryFailedBatch() {
        MsMiddlewareProperties.MqActuatorProperties mq = properties.getAutonomy().getMq();
        return retryFailedBatch(mq.getDelayedRetryBatchSize(), mq.getDelayedRetryDelayMs());
    }

    /**
     * 批量延迟重试失败消息。
     *
     * @param batchSize 单次最多重试条数
     * @param delayMs   延迟毫秒数
     * @return 成功投递的重试消息数量
     */
    public int retryFailedBatch(int batchSize, long delayMs) {
        List<MessageTrace> failed = traceManager.listFailedTraces(batchSize);
        if (failed.isEmpty()) {
            logger.info("无失败消息可延迟重试");
            return 0;
        }

        int success = 0;
        for (MessageTrace trace : failed) {
            if (retryOne(trace, delayMs)) {
                success++;
            }
        }
        logger.info("MQ 延迟重试批次完成: 尝试 {} 条，成功 {} 条", failed.size(), success);
        return success;
    }

    private boolean retryOne(MessageTrace trace, long delayMs) {
        Object payload = extractRetryPayload(trace);
        if (payload == null) {
            logger.debug("跳过无 retryPayload 的失败消息: {}", trace.getMessageId());
            return false;
        }
        String exchange = trace.getExchange() != null ? trace.getExchange() : "ms-exchange";
        String routingKey = trace.getRoutingKey() != null ? trace.getRoutingKey() : "ms-routing-key";
        boolean ok = messageQueue.sendDelayed(exchange, routingKey, payload, delayMs);
        if (ok) {
            traceManager.markRetryScheduled(trace.getMessageId());
        }
        return ok;
    }

    /** 从 trace.extra 取出失败时缓存的消息体 */
    private Object extractRetryPayload(MessageTrace trace) {
        Map<String, Object> extra = trace.getExtra();
        if (extra == null) {
            return null;
        }
        return extra.get(RETRY_PAYLOAD_KEY);
    }

    /** extra 中缓存重试 payload 的键名，供 RabbitMessageQueue 写入 */
    public static String retryPayloadKey() {
        return RETRY_PAYLOAD_KEY;
    }
}
