package com.ms.middleware.cache.warmup;

import com.ms.middleware.cache.MsCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热执行器
 * 用于在应用启动时异步加载热点数据到缓存
 */
public class CacheWarmupExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupExecutor.class);

    private final MsCache cache;
    private final CacheWarmup warmupProvider;
    private final ExecutorService executorService;

    public CacheWarmupExecutor(MsCache cache, CacheWarmup warmupProvider) {
        this.cache = cache;
        this.warmupProvider = warmupProvider;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "cache-warmup-thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 执行缓存预热
     */
    public void executeWarmup() {
        if (!warmupProvider.isEnabled()) {
            logger.info("Cache warmup is disabled");
            return;
        }

        logger.info("Starting cache warmup...");

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> warmupData = warmupProvider.getWarmupData();
                List<CacheWarmup.WarmupItem> warmupItems = warmupProvider.getWarmupItems();

                int successCount = 0;
                int failCount = 0;

                if (!warmupData.isEmpty()) {
                    for (Map.Entry<String, Object> entry : warmupData.entrySet()) {
                        try {
                            cache.put(entry.getKey(), entry.getValue());
                            successCount++;
                        } catch (Exception e) {
                            failCount++;
                            logger.error("Failed to warm up cache for key: {}", entry.getKey(), e);
                        }
                    }
                }

                if (!warmupItems.isEmpty()) {
                    for (CacheWarmup.WarmupItem item : warmupItems) {
                        try {
                            if (item.expireSeconds() > 0) {
                                cache.put(item.key(), item.value(), item.expireSeconds(), TimeUnit.SECONDS);
                            } else {
                                cache.put(item.key(), item.value());
                            }
                            successCount++;
                        } catch (Exception e) {
                            failCount++;
                            logger.error("Failed to warm up cache for key: {}", item.key(), e);
                        }
                    }
                }

                logger.info("Cache warmup completed. Success: {}, Failed: {}", successCount, failCount);
            } catch (Exception e) {
                logger.error("Cache warmup failed", e);
            }
        }, executorService);
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
