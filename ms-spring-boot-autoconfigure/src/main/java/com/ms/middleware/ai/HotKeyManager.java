package com.ms.middleware.ai;

import com.ms.middleware.cache.MultiLevelCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AI热点识别管理器
 * 用于管理热点数据的识别和预热
 */
public class HotKeyManager {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyManager.class);

    private final HotKeyDetector detector;
    private final MultiLevelCache cache;
    private final ScheduledExecutorService executorService;

    public HotKeyManager(HotKeyConfig config, MultiLevelCache cache) {
        this.detector = new HotKeyDetector(config);
        this.cache = cache;
        this.executorService = Executors.newScheduledThreadPool(2);

        // 启动热点数据预热任务
        if (config.isAutoWarmup()) {
            startAutoWarmup(config);
        }
    }

    /**
     * 记录key访问
     * @param key 缓存key
     */
    public void recordAccess(String key) {
        detector.recordAccess(key);
    }

    /**
     * 判断是否为热点key
     * @param key 缓存key
     * @return 是否为热点key
     */
    public boolean isHotKey(String key) {
        return detector.isHotKey(key);
    }

    /**
     * 获取所有热点key
     * @return 热点key集合
     */
    public Set<String> getHotKeys() {
        return detector.getHotKeys();
    }

    /**
     * 启动自动预热任务
     * @param config 配置
     */
    private void startAutoWarmup(HotKeyConfig config) {
        executorService.scheduleAtFixedRate(() -> {
            try {
                Set<String> hotKeys = detector.getHotKeys();
                if (!hotKeys.isEmpty()) {
                    logger.info("Auto-warming up {} hot keys", hotKeys.size());
                    for (String key : hotKeys) {
                        // 检查缓存中是否存在该key
                        Object value = cache.get(key);
                        if (value == null) {
                            // 如果缓存中不存在，尝试从数据源加载
                            // 这里可以集成CacheLoader
                            logger.debug("Hot key {} not in cache, needs warmup", key);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error during auto-warmup", e);
            }
        }, config.getStatisticsIntervalMs(), config.getStatisticsIntervalMs(), TimeUnit.MILLISECONDS);

        logger.info("Auto-warmup task started with interval {} ms", config.getStatisticsIntervalMs());
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("HotKeyManager shutdown");
    }
}
