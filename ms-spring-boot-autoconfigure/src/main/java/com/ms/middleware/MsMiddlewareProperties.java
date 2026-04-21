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
     * 整体缓存开关
     */
    private boolean cacheEnabled = true;

    /**
     * 消息队列配置
     */
    private MqProperties mq = new MqProperties();

    /**
     * Redis配置
     */
    private RedisProperties redis = new RedisProperties();

    /**
     * 安全配置
     */
    private SecurityProperties security = new SecurityProperties();

    /**
     * AI配置
     */
    private AiProperties ai = new AiProperties();

    /**
     * 服务发现配置
     */
    private DiscoveryProperties discovery = new DiscoveryProperties();

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

    public SecurityProperties getSecurity() {
        return security;
    }

    public void setSecurity(SecurityProperties security) {
        this.security = security;
    }

    public DiscoveryProperties getDiscovery() {
        return discovery;
    }

    public void setDiscovery(DiscoveryProperties discovery) {
        this.discovery = discovery;
    }

    /**
     * 配置中心配置
     */
    private ConfigCenterProperties config = new ConfigCenterProperties();

    public ConfigCenterProperties getConfig() {
        return config;
    }

    public void setConfig(ConfigCenterProperties config) {
        this.config = config;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    /**
     * 服务发现配置
     */
    public static class DiscoveryProperties {
        private boolean enabled = true;
        private String serverAddr = "localhost:8848";
        private String namespace = "";
        private String group = "DEFAULT_GROUP";
        private String clusterName = "DEFAULT";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServerAddr() {
            return serverAddr;
        }

        public void setServerAddr(String serverAddr) {
            this.serverAddr = serverAddr;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getClusterName() {
            return clusterName;
        }

        public void setClusterName(String clusterName) {
            this.clusterName = clusterName;
        }
    }

    /**
     * 配置中心配置
     */
    public static class ConfigCenterProperties {
        private boolean enabled = true;
        private String serverAddr = "localhost:8848";
        private String namespace = "";
        private String group = "DEFAULT_GROUP";
        private String fileExtension = "yaml";
        private long timeout = 5000;
        private boolean refreshEnabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServerAddr() {
            return serverAddr;
        }

        public void setServerAddr(String serverAddr) {
            this.serverAddr = serverAddr;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public void setFileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public boolean isRefreshEnabled() {
            return refreshEnabled;
        }

        public void setRefreshEnabled(boolean refreshEnabled) {
            this.refreshEnabled = refreshEnabled;
        }
    }

    /**
     * 缓存配置
     */
    public static class CacheProperties {
        /**
         * 是否启用缓存
         */
        private boolean enabled = true;
        
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
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
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

        /**
         * 是否在应用启动时清空指定队列（仅建议开发环境开启）
         */
        private boolean purgeOnStartup = false;

        /**
         * 启动时需要清空的队列列表
         */
        private java.util.List<String> purgeQueues = new java.util.ArrayList<>();

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

        public boolean isPurgeOnStartup() {
            return purgeOnStartup;
        }

        public void setPurgeOnStartup(boolean purgeOnStartup) {
            this.purgeOnStartup = purgeOnStartup;
        }

        public java.util.List<String> getPurgeQueues() {
            return purgeQueues;
        }

        public void setPurgeQueues(java.util.List<String> purgeQueues) {
            this.purgeQueues = purgeQueues;
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

    /**
     * 安全配置
     */
    public static class SecurityProperties {
        /**
         * 是否启用安全功能
         */
        private boolean enabled = true;

        /**
         * 缓存安全配置
         */
        private CacheSecurityProperties cache = new CacheSecurityProperties();

        /**
         * 消息队列安全配置
         */
        private MqSecurityProperties mq = new MqSecurityProperties();

        /**
         * 锁安全配置
         */
        private LockSecurityProperties lock = new LockSecurityProperties();

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public CacheSecurityProperties getCache() {
            return cache;
        }

        public void setCache(CacheSecurityProperties cache) {
            this.cache = cache;
        }

        public MqSecurityProperties getMq() {
            return mq;
        }

        public void setMq(MqSecurityProperties mq) {
            this.mq = mq;
        }

        public LockSecurityProperties getLock() {
            return lock;
        }

        public void setLock(LockSecurityProperties lock) {
            this.lock = lock;
        }
    }

    /**
     * 缓存安全配置
     */
    public static class CacheSecurityProperties {
        /**
         * 是否启用访问控制
         */
        private boolean accessControlEnabled = true;

        /**
         * 缓存键前缀
         */
        private String keyPrefix = "ms:cache:";

        /**
         * 缓存访问超时时间（毫秒）
         */
        private long accessTimeout = 5000;

        // Getters and Setters
        public boolean isAccessControlEnabled() {
            return accessControlEnabled;
        }

        public void setAccessControlEnabled(boolean accessControlEnabled) {
            this.accessControlEnabled = accessControlEnabled;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public long getAccessTimeout() {
            return accessTimeout;
        }

        public void setAccessTimeout(long accessTimeout) {
            this.accessTimeout = accessTimeout;
        }
    }

    /**
     * 消息队列安全配置
     */
    public static class MqSecurityProperties {
        /**
         * 是否启用消息加密
         */
        private boolean encryptionEnabled = true;

        /**
         * 是否启用权限管理
         */
        private boolean permissionEnabled = true;

        /**
         * 加密密钥
         */
        private String encryptionKey = "ms-middleware-secret-key";

        /**
         * 权限前缀
         */
        private String permissionPrefix = "ms:mq:permission:";

        // Getters and Setters
        public boolean isEncryptionEnabled() {
            return encryptionEnabled;
        }

        public void setEncryptionEnabled(boolean encryptionEnabled) {
            this.encryptionEnabled = encryptionEnabled;
        }

        public boolean isPermissionEnabled() {return permissionEnabled;}

        public void setPermissionEnabled(boolean permissionEnabled) {
            this.permissionEnabled = permissionEnabled;
        }

        public String getEncryptionKey() {
            return encryptionKey;
        }

        public void setEncryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
        }

        public String getPermissionPrefix() {
            return permissionPrefix;
        }

        public void setPermissionPrefix(String permissionPrefix) {
            this.permissionPrefix = permissionPrefix;
        }
    }

    /**
     * 锁安全配置
     */
    public static class LockSecurityProperties {
        /**
         * 是否启用访问控制
         */
        private boolean accessControlEnabled = true;

        /**
         * 锁键前缀
         */
        private String keyPrefix = "ms:lock:";

        /**
         * 锁获取超时时间（毫秒）
         */
        private long acquisitionTimeout = 5000;

        // Getters and Setters
        public boolean isAccessControlEnabled() {
            return accessControlEnabled;
        }

        public void setAccessControlEnabled(boolean accessControlEnabled) {
            this.accessControlEnabled = accessControlEnabled;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public long getAcquisitionTimeout() {
            return acquisitionTimeout;
        }

        public void setAcquisitionTimeout(long acquisitionTimeout) {
            this.acquisitionTimeout = acquisitionTimeout;
        }
    }
}
