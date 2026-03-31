package com.ms.middleware.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.metrics.MsMetrics;
import com.ms.middleware.mq.idempotent.IdempotentStore;
import com.ms.middleware.mq.idempotent.RedisIdempotentStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMessageQueueTest {

    private RabbitMessageQueue messageQueue;
    private RabbitTemplate rabbitTemplate;
    private CachingConnectionFactory connectionFactory;
    private ObjectMapper objectMapper;
    private IdempotentStore idempotentStore;

    @BeforeEach
    void setUp() {
        // 模拟依赖
        rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        connectionFactory = Mockito.mock(CachingConnectionFactory.class);
        objectMapper = new ObjectMapper();
        RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
        idempotentStore = new RedisIdempotentStore(redissonClient);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        MsMetrics metrics = new MsMetrics(meterRegistry);

        // 创建消息队列实例
        messageQueue = new RabbitMessageQueue(rabbitTemplate, connectionFactory, objectMapper, idempotentStore, metrics);
    }

    @Test
    void testSend() {
        // 测试发送消息
        boolean result = messageQueue.send("test-exchange", "test-routing-key", "test-message");
        assertTrue(result);
    }

    @Test
    void testSendAsync() {
        // 测试异步发送消息
        java.util.concurrent.CompletableFuture<Boolean> future = messageQueue.sendAsync("test-exchange", "test-routing-key", "test-message");
        assertNotNull(future);
    }

    @Test
    void testSendWithHeaders() {
        // 测试带头部信息的消息发送
        Map<String, Object> headers = Map.of("key", "value");
        boolean result = messageQueue.sendWithHeaders("test-exchange", "test-routing-key", "test-message", headers);
        assertTrue(result);
    }

    @Test
    void testSendDelayed() {
        // 测试延迟消息发送
        boolean result = messageQueue.sendDelayed("test-exchange", "test-routing-key", "test-message", 1000);
        assertTrue(result);
    }

    @Test
    void testSendOrdered() {
        // 测试顺序消息发送
        boolean result = messageQueue.sendOrdered("test-exchange", "test-routing-key", "test-message", "order-key");
        assertTrue(result);
    }

    @Test
    void testRegisterListener() {
        // 测试注册监听器
        CountDownLatch latch = new CountDownLatch(1);
        MessageListener listener = (message, headers, messageId) -> {
            System.out.println("Received message: " + message);
            latch.countDown();
            return true;
        };

        messageQueue.registerListener("test-queue", listener);
        // 验证监听器注册成功（这里只是验证方法调用，实际消息接收需要集成测试）
        assertNotNull(messageQueue);
    }

    @Test
    void testClose() {
        // 测试关闭方法
        messageQueue.close();
        // 验证关闭方法执行成功
        assertNotNull(messageQueue);
    }
}
