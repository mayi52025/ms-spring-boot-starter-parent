package com.ms.middleware.cache.consistency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存一致性管理器测试类
 */
class CacheConsistencyManagerTest {

    private CacheConsistencyManager manager;
    private TestListener listener;

    @BeforeEach
    void setUp() {
        manager = new CacheConsistencyManager();
        listener = new TestListener();
        manager.addListener(listener);
    }

    @Test
    void testAddListener() {
        assertEquals(1, manager.getListenerCount());
    }

    @Test
    void testRemoveListener() {
        manager.removeListener(listener);
        assertEquals(0, manager.getListenerCount());
    }

    @Test
    void testInvalidateSingle() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        listener.setLatch(latch);

        manager.invalidate("testKey");

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(CacheInvalidationEvent.InvalidationType.SINGLE, listener.getLastEvent().getType());
        assertEquals("testKey", listener.getLastEvent().getKey());
    }

    @Test
    void testInvalidateBatch() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        listener.setLatch(latch);

        Set<String> keys = new HashSet<>();
        keys.add("key1");
        keys.add("key2");
        keys.add("key3");

        manager.invalidateBatch(keys);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(CacheInvalidationEvent.InvalidationType.BATCH, listener.getLastEvent().getType());
        assertEquals(keys, listener.getLastEvent().getKeys());
    }

    @Test
    void testInvalidateAll() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        listener.setLatch(latch);

        manager.invalidateAll();

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(CacheInvalidationEvent.InvalidationType.ALL, listener.getLastEvent().getType());
    }

    @Test
    void testMultipleListeners() throws InterruptedException {
        TestListener listener2 = new TestListener();
        manager.addListener(listener2);

        CountDownLatch latch = new CountDownLatch(2);
        listener.setLatch(latch);
        listener2.setLatch(latch);

        manager.invalidate("testKey");

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(2, manager.getListenerCount());
    }

    @Test
    void testDisabledListener() {
        TestListener disabledListener = new TestListener() {
            @Override
            public boolean isEnabled() {
                return false;
            }
        };

        manager.addListener(disabledListener);
        assertEquals(1, manager.getListenerCount());
    }

    static class TestListener implements CacheInvalidationListener {
        private CacheInvalidationEvent lastEvent;
        private CountDownLatch latch;

        public void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }

        public CacheInvalidationEvent getLastEvent() {
            return lastEvent;
        }

        @Override
        public void onInvalidation(CacheInvalidationEvent event) {
            this.lastEvent = event;
            if (latch != null) {
                latch.countDown();
            }
        }
    }
}
