package com.ms.middleware.ai;

/**
 * AI热点识别配置
 */
public class HotKeyConfig {

    /**
     * 是否启用热点识别
     */
    private boolean enabled = true;

    /**
     * 热点key阈值（访问频率超过此值被认为是热点）
     */
    private double hotKeyThreshold = 0.1;

    /**
     * Top N热点key数量
     */
    private int topN = 10;

    /**
     * 统计间隔（毫秒）
     */
    private long statisticsIntervalMs = 60000;

    /**
     * 是否自动预热热点数据
     */
    private boolean autoWarmup = true;

    /**
     * 热点数据过期时间（秒）
     */
    private int hotKeyExpireSeconds = 3600;

    public HotKeyConfig() {
    }

    public HotKeyConfig(boolean enabled, double hotKeyThreshold, int topN, long statisticsIntervalMs, boolean autoWarmup, int hotKeyExpireSeconds) {
        this.enabled = enabled;
        this.hotKeyThreshold = hotKeyThreshold;
        this.topN = topN;
        this.statisticsIntervalMs = statisticsIntervalMs;
        this.autoWarmup = autoWarmup;
        this.hotKeyExpireSeconds = hotKeyExpireSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getHotKeyThreshold() {
        return hotKeyThreshold;
    }

    public void setHotKeyThreshold(double hotKeyThreshold) {
        this.hotKeyThreshold = hotKeyThreshold;
    }

    public int getTopN() {
        return topN;
    }

    public void setTopN(int topN) {
        this.topN = topN;
    }

    public long getStatisticsIntervalMs() {
        return statisticsIntervalMs;
    }

    public void setStatisticsIntervalMs(long statisticsIntervalMs) {
        this.statisticsIntervalMs = statisticsIntervalMs;
    }

    public boolean isAutoWarmup() {
        return autoWarmup;
    }

    public void setAutoWarmup(boolean autoWarmup) {
        this.autoWarmup = autoWarmup;
    }

    public int getHotKeyExpireSeconds() {
        return hotKeyExpireSeconds;
    }

    public void setHotKeyExpireSeconds(int hotKeyExpireSeconds) {
        this.hotKeyExpireSeconds = hotKeyExpireSeconds;
    }

    /**
     * 创建默认配置
     * @return 默认配置
     */
    public static HotKeyConfig defaultConfig() {
        return new HotKeyConfig();
    }

    @Override
    public String toString() {
        return "HotKeyConfig{" +
                "enabled=" + enabled +
                ", hotKeyThreshold=" + hotKeyThreshold +
                ", topN=" + topN +
                ", statisticsIntervalMs=" + statisticsIntervalMs +
                ", autoWarmup=" + autoWarmup +
                ", hotKeyExpireSeconds=" + hotKeyExpireSeconds +
                '}';
    }
}
