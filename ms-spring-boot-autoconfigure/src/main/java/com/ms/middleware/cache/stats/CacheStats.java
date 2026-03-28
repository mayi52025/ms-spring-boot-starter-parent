package com.ms.middleware.cache.stats;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存统计信息
 * 用于记录缓存的命中率、大小、操作次数等关键指标
 */
public class CacheStats {

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong putCount = new AtomicLong(0);
    private final AtomicLong removeCount = new AtomicLong(0);
    private final AtomicLong clearCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    public void recordHit() {
        hitCount.incrementAndGet();
    }

    public void recordMiss() {
        missCount.incrementAndGet();
    }

    public void recordPut() {
        putCount.incrementAndGet();
    }

    public void recordRemove() {
        removeCount.incrementAndGet();
    }

    public void recordClear() {
        clearCount.incrementAndGet();
    }

    public void recordError() {
        errorCount.incrementAndGet();
    }

    public long getHitCount() {
        return hitCount.get();
    }

    public long getMissCount() {
        return missCount.get();
    }

    public long getRequestCount() {
        return hitCount.get() + missCount.get();
    }

    public long getPutCount() {
        return putCount.get();
    }

    public long getRemoveCount() {
        return removeCount.get();
    }

    public long getClearCount() {
        return clearCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    public double getHitRate() {
        long requestCount = getRequestCount();
        return requestCount == 0 ? 0.0 : (double) hitCount.get() / requestCount;
    }

    public double getMissRate() {
        long requestCount = getRequestCount();
        return requestCount == 0 ? 0.0 : (double) missCount.get() / requestCount;
    }

    public void reset() {
        hitCount.set(0);
        missCount.set(0);
        putCount.set(0);
        removeCount.set(0);
        clearCount.set(0);
        errorCount.set(0);
    }

    @Override
    public String toString() {
        return String.format(
            "CacheStats{hitCount=%d, missCount=%d, hitRate=%.2f%%, putCount=%d, removeCount=%d, clearCount=%d, errorCount=%d}",
            getHitCount(), getMissCount(), getHitRate() * 100,
            getPutCount(), getRemoveCount(), getClearCount(), getErrorCount()
        );
    }
}
