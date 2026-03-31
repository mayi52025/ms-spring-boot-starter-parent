package com.ms.middleware.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.metrics.MsMetrics;
import com.ms.middleware.mq.idempotent.IdempotentConsumer;
import com.ms.middleware.mq.idempotent.IdempotentStore;
import com.ms.middleware.mq.trace.MessageTrace;
import com.ms.middleware.mq.trace.MessageTraceManager;
import com.ms.middleware.security.SecurityUtils;
import io.micrometer.core.instrument.Timer;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 基于RabbitMQ的消息队列实现
 */
public class RabbitMessageQueue implements MsMessageQueue {

    private final RabbitTemplate rabbitTemplate;
    private final ConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;
    private final MessageTraceManager traceManager;
    private final IdempotentStore idempotentStore;
    private final MsMetrics metrics;
    private final MsMiddlewareProperties.SecurityProperties securityProperties;

    public RabbitMessageQueue(RabbitTemplate rabbitTemplate, 
                           ConnectionFactory connectionFactory, 
                           ObjectMapper objectMapper, 
                           IdempotentStore idempotentStore, 
                           MsMiddlewareProperties properties, 
                           MsMetrics metrics) {
        this.rabbitTemplate = rabbitTemplate;
        this.connectionFactory = connectionFactory;
        this.objectMapper = objectMapper;
        this.traceManager = MessageTraceManager.getInstance();
        this.idempotentStore = idempotentStore;
        this.securityProperties = properties.getSecurity();
        this.metrics = metrics;
        
        // 配置消息转换器
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
    }

    /**
     * 加密消息
     * @param message 原始消息
     * @return 加密后的消息
     */
    private String encryptMessage(Object message) {
        try {
            if (securityProperties.isEnabled() && securityProperties.getMq().isEncryptionEnabled()) {
                String messageStr = objectMapper.writeValueAsString(message);
                return SecurityUtils.encryptAES(messageStr, securityProperties.getMq().getEncryptionKey());
            }
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt message", e);
        }
    }

    /**
     * 解密消息
     * @param encryptedMessage 加密后的消息
     * @param targetType 目标类型
     * @return 解密后的消息
     */
    private <T> T decryptMessage(String encryptedMessage, Class<T> targetType) {
        try {
            if (securityProperties.isEnabled() && securityProperties.getMq().isEncryptionEnabled()) {
                String decryptedStr = SecurityUtils.decryptAES(encryptedMessage, securityProperties.getMq().getEncryptionKey());
                return objectMapper.readValue(decryptedStr, targetType);
            }
            return objectMapper.readValue(encryptedMessage, targetType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt message", e);
        }
    }

    @Override
    public boolean send(String exchange, String routingKey, Object message) {
        try {
            String messageId = UUID.randomUUID().toString();
            
            // 记录消息发送
            MessageTrace trace = MessageTrace.builder()
                .messageId(messageId)
                .sendTime(Instant.now())
                .sender(getSender())
                .exchange(exchange)
                .routingKey(routingKey)
                .build();
            traceManager.recordSend(trace);

            // 加密消息
            String encryptedMessage = encryptMessage(message);

            // 发送消息
            rabbitTemplate.convertAndSend(exchange, routingKey, encryptedMessage, msg -> {
                msg.getMessageProperties().setMessageId(messageId);
                msg.getMessageProperties().setTimestamp(new java.util.Date());
                msg.getMessageProperties().setHeader("encrypted", securityProperties.isEnabled() && securityProperties.getMq().isEncryptionEnabled());
                return msg;
            });
            metrics.incrementMessagePublished();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            metrics.incrementFailureCount();
            return false;
        }
    }

    @Override
    public CompletableFuture<Boolean> sendAsync(String exchange, String routingKey, Object message) {
        return CompletableFuture.supplyAsync(() -> send(exchange, routingKey, message));
    }

    @Override
    public boolean sendWithHeaders(String exchange, String routingKey, Object message, Map<String, Object> headers) {
        try {
            String messageId = UUID.randomUUID().toString();
            
            // 记录消息发送
            MessageTrace trace = MessageTrace.builder()
                .messageId(messageId)
                .sendTime(Instant.now())
                .sender(getSender())
                .exchange(exchange)
                .routingKey(routingKey)
                .build();
            traceManager.recordSend(trace);

            // 加密消息
            String encryptedMessage = encryptMessage(message);

            // 发送消息
            rabbitTemplate.convertAndSend(exchange, routingKey, encryptedMessage, msg -> {
                msg.getMessageProperties().setMessageId(messageId);
                msg.getMessageProperties().setTimestamp(new java.util.Date());
                msg.getMessageProperties().setHeader("encrypted", securityProperties.isEnabled() && securityProperties.getMq().isEncryptionEnabled());
                if (headers != null) {
                    msg.getMessageProperties().getHeaders().putAll(headers);
                }
                return msg;
            });
            metrics.incrementMessagePublished();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            metrics.incrementFailureCount();
            return false;
        }
    }

    @Override
    public boolean sendDelayed(String exchange, String routingKey, Object message, long delay) {
        try {
            String messageId = UUID.randomUUID().toString();
            
            // 记录消息发送
            MessageTrace trace = MessageTrace.builder()
                .messageId(messageId)
                .sendTime(Instant.now())
                .sender(getSender())
                .exchange(exchange)
                .routingKey(routingKey)
                .build();
            traceManager.recordSend(trace);

            // 加密消息
            String encryptedMessage = encryptMessage(message);

            // 发送延迟消息
            rabbitTemplate.convertAndSend(exchange, routingKey, encryptedMessage, msg -> {
                msg.getMessageProperties().setMessageId(messageId);
                msg.getMessageProperties().setTimestamp(new java.util.Date());
                msg.getMessageProperties().setHeader("x-delay", delay);
                msg.getMessageProperties().setHeader("encrypted", securityProperties.isEnabled() && securityProperties.getMq().isEncryptionEnabled());
                return msg;
            });
            metrics.incrementMessagePublished();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            metrics.incrementFailureCount();
            return false;
        }
    }

    @Override
    public boolean sendOrdered(String exchange, String routingKey, Object message, String orderKey) {
        try {
            String messageId = UUID.randomUUID().toString();
            
            // 记录消息发送
            MessageTrace trace = MessageTrace.builder()
                .messageId(messageId)
                .sendTime(Instant.now())
                .sender(getSender())
                .exchange(exchange)
                .routingKey(routingKey)
                .build();
            traceManager.recordSend(trace);

            // 加密消息
            String encryptedMessage = encryptMessage(message);

            // 发送顺序消息（使用orderKey作为分区键）
            rabbitTemplate.convertAndSend(exchange, routingKey, encryptedMessage, msg -> {
                msg.getMessageProperties().setMessageId(messageId);
                msg.getMessageProperties().setTimestamp(new java.util.Date());
                msg.getMessageProperties().setHeader("order-key", orderKey);
                msg.getMessageProperties().setHeader("encrypted", securityProperties.isEnabled() && securityProperties.getMq().isEncryptionEnabled());
                return msg;
            });
            metrics.incrementMessagePublished();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            metrics.incrementFailureCount();
            return false;
        }
    }

    @Override
    public void registerListener(String queueName, MessageListener listener) {
        registerListener(queueName, listener, ListenerConfig.defaultConfig());
    }

    @Override
    public void registerListener(String queueName, MessageListener listener, ListenerConfig config) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(queueName);
        container.setConcurrentConsumers(config.getConcurrency());
        container.setMaxConcurrentConsumers(config.getConcurrency());
        container.setAutoStartup(true);

        // 创建消息监听器适配器
        MessageListenerAdapter adapter = new MessageListenerAdapter(new org.springframework.amqp.core.MessageListener() {
            @Override
            public void onMessage(org.springframework.amqp.core.Message message) {
                handleMessage(message, listener, config);
            }
        });
        adapter.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));

        container.setMessageListener(adapter);
        container.start();
    }

    private void handleMessage(org.springframework.amqp.core.Message message, MessageListener listener, ListenerConfig config) {
        String messageId = message.getMessageProperties().getMessageId();
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        Object payload = null;

        try {
            // 反序列化并解密消息
            String messageBody = new String(message.getBody());
            Boolean isEncrypted = (Boolean) headers.get("encrypted");
            if (isEncrypted != null && isEncrypted) {
                payload = decryptMessage(messageBody, Object.class);
            } else {
                payload = objectMapper.readValue(message.getBody(), Object.class);
            }

            // 记录消息接收
            traceManager.recordReceive(messageId, message.getMessageProperties().getConsumerQueue(), getReceiver());

            // 开始记录消息处理时间
            Timer.Sample sample = metrics.startMessageProcessing();
            long startTime = System.currentTimeMillis();
            boolean success;

            // 处理幂等消费
            if (config.isIdempotent() && idempotentStore != null) {
                IdempotentConsumer idempotentConsumer = new IdempotentConsumer(
                    idempotentStore, 
                    config.getIdempotentPrefix(),
                    config.getTimeout().toMillis()
                );
                success = idempotentConsumer.process(messageId, payload, headers, listener);
            } else {
                success = listener.onMessage(payload, headers, messageId);
            }

            long processTime = System.currentTimeMillis() - startTime;
            traceManager.recordProcess(messageId, success, null, processTime);
            
            // 停止记录消息处理时间
            metrics.stopMessageProcessing(sample);
            metrics.incrementMessageConsumed();

        } catch (Exception e) {
            e.printStackTrace();
            traceManager.recordProcess(messageId, false, e.getMessage(), 0);
            metrics.incrementMessageFailed();
            metrics.incrementFailureCount();
        }
    }

    @Override
    public void close() {
        // RabbitTemplate不需要手动关闭
    }

    private String getSender() {
        return "ms-middleware-sender";
    }

    private String getReceiver() {
        return "ms-middleware-receiver";
    }
}
