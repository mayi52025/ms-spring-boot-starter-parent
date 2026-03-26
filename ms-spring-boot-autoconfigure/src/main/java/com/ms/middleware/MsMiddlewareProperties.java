package com.ms.middleware;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 中间件配置属性类
 * 用于管理 Redis、RabbitMQ、缓存、AI 等中间件的配置
 */
@ConfigurationProperties(prefix = "ms.middleware")
public class MsMiddlewareProperties {

    /**
     * 缓存配置
     */
    private CacheProperties cache = new CacheProperties();

    /**
     * 消息队列配置
     */
    private MqProperties mq = new MqProperties();

    /**
     * AI 服务配置
     */
    private AiProperties ai = new AiProperties();

    /**
     * 环境配置
     */
    private EnvProperties env = new EnvProperties();

    // Getters and Setters
    public CacheProperties getCache() {
        return cache;
    }

    public void setCache(CacheProperties cache) {
        this.cache = cache;
    }

    public MqProperties getMq() {
        return mq;
    }

    public void setMq(MqProperties mq) {
        this.mq = mq;
    }

    public AiProperties getAi() {
        return ai;
    }

    public void setAi(AiProperties ai) {
        this.ai = ai;
    }

    public EnvProperties getEnv() {
        return env;
    }

    public void setEnv(EnvProperties env) {
        this.env = env;
    }

    /**
     * 缓存配置
     */
    public static class CacheProperties {
        /**
         * 本地缓存配置
         */
        private LocalCacheProperties local = new LocalCacheProperties();

        /**
         * 分布式缓存配置
         */
        private DistributedCacheProperties distributed = new DistributedCacheProperties();

        // Getters and Setters
        public LocalCacheProperties getLocal() {
            return local;
        }

        public void setLocal(LocalCacheProperties local) {
            this.local = local;
        }

        public DistributedCacheProperties getDistributed() {
            return distributed;
        }

        public void setDistributed(DistributedCacheProperties distributed) {
            this.distributed = distributed;
        }
    }

    /**
     * 本地缓存配置
     */
    public static class LocalCacheProperties {
        /**
         * 缓存大小
         */
        private int size = 1000;

        /**
         * 缓存过期时间（秒）
         */
        private int ttl = 60;

        /**
         * 缓存刷新时间（秒）
         */
        private int refreshInterval = 30;

        // Getters and Setters
        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getTtl() {
            return ttl;
        }

        public void setTtl(int ttl) {
            this.ttl = ttl;
        }

        public int getRefreshInterval() {
            return refreshInterval;
        }

        public void setRefreshInterval(int refreshInterval) {
            this.refreshInterval = refreshInterval;
        }
    }

    /**
     * 分布式缓存配置
     */
    public static class DistributedCacheProperties {
        /**
         * 缓存过期时间（秒）
         */
        private int ttl = 300;

        /**
         * 是否启用
         */
        private boolean enabled = true;

        // Getters and Setters
        public int getTtl() {
            return ttl;
        }

        public void setTtl(int ttl) {
            this.ttl = ttl;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 消息队列配置
     */
    public static class MqProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 死信队列配置
         */
        private DeadLetterProperties deadLetter = new DeadLetterProperties();

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public DeadLetterProperties getDeadLetter() {
            return deadLetter;
        }

        public void setDeadLetter(DeadLetterProperties deadLetter) {
            this.deadLetter = deadLetter;
        }
    }

    /**
     * 死信队列配置
     */
    public static class DeadLetterProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 死信交换机名称
         */
        private String exchange = "ms.dead.letter.exchange";

        /**
         * 死信路由键
         */
        private String routingKey = "ms.dead.letter.key";

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }
    }

    /**
     * AI 服务配置
     */
    public static class AiProperties {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * AI 服务地址
         */
        private String url = "http://localhost:8000";

        /**
         * 热点识别配置
         */
        private HotspotProperties hotspot = new HotspotProperties();

        /**
         * 故障预测配置
         */
        private FaultPredictionProperties faultPrediction = new FaultPredictionProperties();

        /**
         * 消息轨迹分析配置
         */
        private MessageTraceProperties messageTrace = new MessageTraceProperties();

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public HotspotProperties getHotspot() {
            return hotspot;
        }

        public void setHotspot(HotspotProperties hotspot) {
            this.hotspot = hotspot;
        }

        public FaultPredictionProperties getFaultPrediction() {
            return faultPrediction;
        }

        public void setFaultPrediction(FaultPredictionProperties faultPrediction) {
            this.faultPrediction = faultPrediction;
        }

        public MessageTraceProperties getMessageTrace() {
            return messageTrace;
        }

        public void setMessageTrace(MessageTraceProperties messageTrace) {
            this.messageTrace = messageTrace;
        }
    }

    /**
     * 热点识别配置
     */
    public static class HotspotProperties {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * 预测间隔（秒）
         */
        private int predictInterval = 60;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPredictInterval() {
            return predictInterval;
        }

        public void setPredictInterval(int predictInterval) {
            this.predictInterval = predictInterval;
        }
    }

    /**
     * 故障预测配置
     */
    public static class FaultPredictionProperties {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * 预测间隔（秒）
         */
        private int predictInterval = 300;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPredictInterval() {
            return predictInterval;
        }

        public void setPredictInterval(int predictInterval) {
            this.predictInterval = predictInterval;
        }
    }

    /**
     * 消息轨迹分析配置
     */
    public static class MessageTraceProperties {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 环境配置
     */
    public static class EnvProperties {
        /**
         * 环境类型：dev、test、prod
         */
        private String type = "dev";

        /**
         * 是否启用本地开发模式
         */
        private boolean localDevMode = true;

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isLocalDevMode() {
            return localDevMode;
        }

        public void setLocalDevMode(boolean localDevMode) {
            this.localDevMode = localDevMode;
        }
    }
}