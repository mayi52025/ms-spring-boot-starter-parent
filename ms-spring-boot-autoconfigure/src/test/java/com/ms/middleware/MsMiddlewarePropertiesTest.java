package com.ms.middleware;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 中间件配置属性测试
 */
@SpringBootTest(classes = TestConfiguration.class)
public class MsMiddlewarePropertiesTest {

    @Autowired
    private MsMiddlewareProperties properties;

    @Test
    public void testDefaultValues() {
        // 测试环境配置
        assertEquals("dev", properties.getEnv().getType());
        assertTrue(properties.getEnv().isLocalDevMode());

        // 测试本地缓存配置
        assertEquals(1000, properties.getCache().getLocal().getSize());
        assertEquals(60, properties.getCache().getLocal().getTtl());
        assertEquals(30, properties.getCache().getLocal().getRefreshInterval());

        // 测试分布式缓存配置
        assertEquals(300, properties.getCache().getDistributed().getTtl());
        assertTrue(properties.getCache().getDistributed().isEnabled());

        // 测试消息队列配置
        assertTrue(properties.getMq().isEnabled());
        assertTrue(properties.getMq().getDeadLetter().isEnabled());
        assertEquals("ms.dead.letter.exchange", properties.getMq().getDeadLetter().getExchange());
        assertEquals("ms.dead.letter.key", properties.getMq().getDeadLetter().getRoutingKey());

        // 测试 AI 服务配置
        assertEquals(false, properties.getAi().isEnabled());
        assertEquals("http://localhost:8000", properties.getAi().getUrl());
        assertEquals(false, properties.getAi().getHotspot().isEnabled());
        assertEquals(60, properties.getAi().getHotspot().getPredictInterval());
        assertEquals(false, properties.getAi().getFaultPrediction().isEnabled());
        assertEquals(300, properties.getAi().getFaultPrediction().getPredictInterval());
        assertEquals(false, properties.getAi().getMessageTrace().isEnabled());
    }
}

/**
 * 测试配置类
 */
@Configuration
@EnableConfigurationProperties(MsMiddlewareProperties.class)
class TestConfiguration {
}