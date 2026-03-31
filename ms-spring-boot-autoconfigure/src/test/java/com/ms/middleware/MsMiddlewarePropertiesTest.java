package com.ms.middleware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MsMiddlewarePropertiesTest {

    @Test
    void testDefaultValues() {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        
        // 测试缓存配置
        assertNotNull(properties.getCache());
        assertNotNull(properties.getCache().getLocal());
        assertEquals(1000, properties.getCache().getLocal().getSize());
        assertEquals(3600, properties.getCache().getLocal().getTtl());
        assertTrue(properties.getCache().getLocal().isEnabled());
        
        assertNotNull(properties.getCache().getDistributed());
        assertEquals(7200, properties.getCache().getDistributed().getTtl());
        assertTrue(properties.getCache().getDistributed().isEnabled());
        
        assertNotNull(properties.getCache().getConsistency());
        assertFalse(properties.getCache().getConsistency().isEnabled());
        assertTrue(properties.getCache().getConsistency().isMultiLevelEnabled());
        
        // 测试消息队列配置
        assertNotNull(properties.getMq());
        assertTrue(properties.getMq().isEnabled());
        
        assertNotNull(properties.getMq().getRabbit());
        assertEquals("192.168.100.102", properties.getMq().getRabbit().getHost());
        assertEquals(5672, properties.getMq().getRabbit().getPort());
        assertEquals("guest", properties.getMq().getRabbit().getUsername());
        assertEquals("guest", properties.getMq().getRabbit().getPassword());
        assertEquals("/", properties.getMq().getRabbit().getVirtualHost());
        assertEquals(30000, properties.getMq().getRabbit().getConnectionTimeout());
        assertTrue(properties.getMq().getRabbit().isAutoDeclare());
        
        assertNotNull(properties.getMq().getTrace());
        assertTrue(properties.getMq().getTrace().isEnabled());
        assertEquals(10000, properties.getMq().getTrace().getMaxCacheSize());
        assertEquals(30, properties.getMq().getTrace().getExpirationMinutes());
        
        assertNotNull(properties.getMq().getIdempotent());
        assertTrue(properties.getMq().getIdempotent().isEnabled());
        assertEquals("mq:idempotent:", properties.getMq().getIdempotent().getPrefix());
        assertEquals(24, properties.getMq().getIdempotent().getExpirationHours());
    }

    @Test
    void testSetters() {
        MsMiddlewareProperties properties = new MsMiddlewareProperties();
        
        // 测试缓存配置设置
        MsMiddlewareProperties.CacheProperties cacheProperties = new MsMiddlewareProperties.CacheProperties();
        properties.setCache(cacheProperties);
        assertNotNull(properties.getCache());
        
        // 测试消息队列配置设置
        MsMiddlewareProperties.MqProperties mqProperties = new MsMiddlewareProperties.MqProperties();
        properties.setMq(mqProperties);
        assertNotNull(properties.getMq());
    }
}
