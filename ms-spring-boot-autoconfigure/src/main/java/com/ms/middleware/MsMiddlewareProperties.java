package com.ms.middleware;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 中间件配置属性
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
     * Redis配置
     */
    private RedisProperties redis = new RedisProperties();

    /**
     * AI配置
     */
    private AiProperties ai = new AiProperties();

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

    public RedisProperties getRedis() {
        return redis;
    }

    public void setRedis(RedisProperties redis) {
        this.redis = redis;
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

        /**
         * 缓存一致性配置
         */
        private ConsistencyProperties consistency = new ConsistencyProperties();

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

        public ConsistencyProperties getConsistency() {
            return consistency;
        }

        public void setConsistency(ConsistencyProperties consistency) {
            this.consistency = consistency;
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
        private int ttl = 3600;

        /**
         * 是否启用
         */
        private boolean enabled = true;

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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 分布式缓存配置
     */
    public static class DistributedCacheProperties {
        /**
         * 缓存过期时间（秒）
         */
        private int ttl = 7200;

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
     * 缓存一致性配置
     */
    public static class ConsistencyProperties {
        /**
         * 是否启用一致性
         */
        private boolean enabled = false;

        /**
         * 是否启用多级缓存一致性
         */
        private boolean multiLevelEnabled = true;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isMultiLevelEnabled() {
            return multiLevelEnabled;
        }

        public void setMultiLevelEnabled(boolean multiLevelEnabled) {
            this.multiLevelEnabled = multiLevelEnabled;
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
         * RabbitMQ配置
         */
        private RabbitProperties rabbit = new RabbitProperties();

        /**
         * 消息追踪配置
         */
        private TraceProperties trace = new TraceProperties();

        /**
         * 幂等配置
         */
        private IdempotentProperties idempotent = new IdempotentProperties();

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public RabbitProperties getRabbit() {
            return rabbit;
        }

        public void setRabbit(RabbitProperties rabbit) {
            this.rabbit = rabbit;
        }

        public TraceProperties getTrace() {
            return trace;
        }

        public void setTrace(TraceProperties trace) {
            this.trace = trace;
        }

        public IdempotentProperties getIdempotent() {
            return idempotent;
        }

        public void setIdempotent(IdempotentProperties idempotent) {
            this.idempotent = idempotent;
        }
    }

    /**
     * RabbitMQ配置
     */
    public static class RabbitProperties {
        /**
         * 主机
         */
        private String host = "192.168.100.102";

        /**
         * 端口
         */
        private int port = 5672;

        /**
         * 用户名
         */
        private String username = "guest";

        /**
         * 密码
         */
        private String password = "guest";

        /**
         * 虚拟主机
         */
        private String virtualHost = "/";

        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 30000;

        /**
         * 自动声明交换机和队列
         */
        private boolean autoDeclare = true;

        // Getters and Setters
        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getVirtualHost() {
            return virtualHost;
        }

        public void setVirtualHost(String virtualHost) {
            this.virtualHost = virtualHost;
        }

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public boolean isAutoDeclare() {
            return autoDeclare;
        }

        public void setAutoDeclare(boolean autoDeclare) {
            this.autoDeclare = autoDeclare;
        }
    }

    /**
     * 消息追踪配置
     */
    public static class TraceProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 最大缓存大小
         */
        private int maxCacheSize = 10000;

        /**
         * 追踪过期时间（分钟）
         */
        private int expirationMinutes = 30;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxCacheSize() {
            return maxCacheSize;
        }

        public void setMaxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
        }

        public int getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(int expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }

    /**
     * 幂等配置
     */
    public static class IdempotentProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 幂等键前缀
         */
        private String prefix = "mq:idempotent:";

        /**
         * 过期时间（小时）
         */
        private int expirationHours = 24;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public int getExpirationHours() {
            return expirationHours;
        }

        public void setExpirationHours(int expirationHours) {
            this.expirationHours = expirationHours;
        }
    }

    /**
     * AI配置
     */
    public static class AiProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 热点识别配置
         */
        private HotKeyProperties hotKey = new HotKeyProperties();

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public HotKeyProperties getHotKey() {
            return hotKey;
        }

        public void setHotKey(HotKeyProperties hotKey) {
            this.hotKey = hotKey;
        }
    }

    /**
     * 热点识别配置
     */
    public static class HotKeyProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 热点key阈值（访问频率超过此值被认为是热点）
         */
        private double threshold = 0.1;

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
        private int expireSeconds = 3600;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
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

        public int getExpireSeconds() {
            return expireSeconds;
        }

        public void setExpireSeconds(int expireSeconds) {
            this.expireSeconds = expireSeconds;
        }
    }

    /**
     * Redis配置
     */
    public static class RedisProperties {
        /**
         * 主机
         */
        private String host = "192.168.100.102";

        /**
         * 端口
         */
        private int port = 6379;

        /**
         * 密码
         */
        private String password = "1234";

        /**
         * 数据库
         */
        private int database = 0;

        // Getters and Setters
        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }
    }
}
