package com.ms.middleware.cache.stats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存统计测试类
 */
class CacheStatsTest {

    private CacheStats stats;

    @BeforeEach
    void setUp() {
        stats = new CacheStats();
    }

    @Test
    void testRecordHit() {
        stats.recordHit();
        stats.recordHit();
        stats.recordHit();

        assertEquals(3, stats.getHitCount());
        assertEquals(0, stats.getMissCount());
        assertEquals(3, stats.getRequestCount());
        assertEquals(1.0, stats.getHitRate());
        assertEquals(0.0, stats.getMissRate());
    }

    @Test
    void testRecordMiss() {
        stats.recordMiss();
        stats.recordMiss();

        assertEquals(0, stats.getHitCount());
        assertEquals(2, stats.getMissCount());
        assertEquals(2, stats.getRequestCount());
        assertEquals(0.0, stats.getHitRate());
        assertEquals(1.0, stats.getMissRate());
    }

    @Test
    void testHitRate() {
        stats.recordHit();
        stats.recordHit();
        stats.recordHit();
        stats.recordMiss();
        stats.recordMiss();

        assertEquals(3, stats.getHitCount());
        assertEquals(2, stats.getMissCount());
        assertEquals(5, stats.getRequestCount());
        assertEquals(0.6, stats.getHitRate(), 0.001);
        assertEquals(0.4, stats.getMissRate(), 0.001);
    }

    @Test
    void testRecordPut() {
        stats.recordPut();
        stats.recordPut();

        assertEquals(2, stats.getPutCount());
    }

    @Test
    void testRecordRemove() {
        stats.recordRemove();
        stats.recordRemove();
        stats.recordRemove();

        assertEquals(3, stats.getRemoveCount());
    }

    @Test
    void testRecordClear() {
        stats.recordClear();

        assertEquals(1, stats.getClearCount());
    }

    @Test
    void testRecordError() {
        stats.recordError();
        stats.recordError();

        assertEquals(2, stats.getErrorCount());
    }

    @Test
    void testReset() {
        stats.recordHit();
        stats.recordHit();
        stats.recordMiss();
        stats.recordPut();
        stats.recordRemove();
        stats.recordClear();
        stats.recordError();

        stats.reset();

        assertEquals(0, stats.getHitCount());
        assertEquals(0, stats.getMissCount());
        assertEquals(0, stats.getPutCount());
        assertEquals(0, stats.getRemoveCount());
        assertEquals(0, stats.getClearCount());
        assertEquals(0, stats.getErrorCount());
    }

    @Test
    void testToString() {
        stats.recordHit();
        stats.recordHit();
        stats.recordMiss();
        stats.recordPut();
        stats.recordRemove();
        stats.recordClear();
        stats.recordError();

        String str = stats.toString();
        assertTrue(str.contains("hitCount=2"));
        assertTrue(str.contains("missCount=1"));
        assertTrue(str.contains("hitRate=66.67%"));
        assertTrue(str.contains("putCount=1"));
        assertTrue(str.contains("removeCount=1"));
        assertTrue(str.contains("clearCount=1"));
        assertTrue(str.contains("errorCount=1"));
    }
}
