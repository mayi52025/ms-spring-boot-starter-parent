package com.ms.middleware.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * AI热点识别器
 * 用于自动识别缓存中的热点数据
 */
public class HotKeyDetector {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyDetector.class);

    // 访问计数器
    private final Map<String, AtomicLong> accessCounter = new ConcurrentHashMap<>();

    // 热点key集合
    private final Set<String> hotKeys = ConcurrentHashMap.newKeySet();

    // 配置参数
    private final HotKeyConfig config;

    // 上次统计时间
    private volatile long lastStatisticsTime = System.currentTimeMillis();

    public HotKeyDetector(HotKeyConfig config) {
        this.config = config;
    }

    /**
     * 记录key访问
     * @param key 缓存key
     */
    public void recordAccess(String key) {
        if (!config.isEnabled()) {
            return;
        }

        accessCounter.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();

        // 检查是否需要统计
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStatisticsTime >= config.getStatisticsIntervalMs()) {
            synchronized (this) {
                if (currentTime - lastStatisticsTime >= config.getStatisticsIntervalMs()) {
                    analyzeHotKeys();
                    lastStatisticsTime = currentTime;
                }
            }
        }
    }

    /**
     * 分析热点key
     */
    private void analyzeHotKeys() {
        if (accessCounter.isEmpty()) {
            return;
        }

        // 计算总访问量
        long totalAccess = accessCounter.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();

        if (totalAccess == 0) {
            return;
        }

        // 计算每个key的访问频率
        Map<String, Double> accessFrequency = new HashMap<>();
        for (Map.Entry<String, AtomicLong> entry : accessCounter.entrySet()) {
            double frequency = (double) entry.getValue().get() / totalAccess;
            accessFrequency.put(entry.getKey(), frequency);
        }

        // 识别热点key（访问频率超过阈值）
        Set<String> newHotKeys = accessFrequency.entrySet().stream()
                .filter(e -> e.getValue() >= config.getHotKeyThreshold())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // 识别Top N热点key
        List<Map.Entry<String, Double>> sortedEntries = accessFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(config.getTopN())
                .collect(Collectors.toList());

        for (Map.Entry<String, Double> entry : sortedEntries) {
            newHotKeys.add(entry.getKey());
        }

        // 更新热点key集合
        hotKeys.clear();
        hotKeys.addAll(newHotKeys);

        // 清理计数器
        accessCounter.clear();

        // 记录日志
        if (!hotKeys.isEmpty()) {
            logger.info("Detected {} hot keys: {}", hotKeys.size(), hotKeys);
        }
    }

    /**
     * 判断是否为热点key
     * @param key 缓存key
     * @return 是否为热点key
     */
    public boolean isHotKey(String key) {
        return hotKeys.contains(key);
    }

    /**
     * 获取所有热点key
     * @return 热点key集合
     */
    public Set<String> getHotKeys() {
        return new HashSet<>(hotKeys);
    }

    /**
     * 获取key的访问次数
     * @param key 缓存key
     * @return 访问次数
     */
    public long getAccessCount(String key) {
        AtomicLong counter = accessCounter.get(key);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 重置统计
     */
    public void reset() {
        accessCounter.clear();
        hotKeys.clear();
        lastStatisticsTime = System.currentTimeMillis();
        logger.info("Hot key detector reset");
    }
}
