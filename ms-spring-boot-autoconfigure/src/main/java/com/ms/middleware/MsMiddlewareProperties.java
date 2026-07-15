package com.ms.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

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

    /**
     * 中间件自治配置
     */
    private AutonomyProperties autonomy = new AutonomyProperties();

    /**
     * AI 控制台配置
     */
    private ConsoleProperties console = new ConsoleProperties();

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

    public AutonomyProperties getAutonomy() {
        return autonomy;
    }

    public void setAutonomy(AutonomyProperties autonomy) {
        this.autonomy = autonomy;
    }

    public ConsoleProperties getConsole() {
        return console;
    }

    public void setConsole(ConsoleProperties console) {
        this.console = console;
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
         * 密码（未配置则为空，勿设错误默认值）
         */
        private String password;

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

    /**
     * 中间件自治（检测、计划、执行、账本）。
     * 总开关 {@code enabled}；扫描间隔、风险阈值、账本类型见子字段。
     */
    public static class AutonomyProperties {
        private boolean enabled = false;
        private long scanIntervalMs = 30000;
        private String autoExecuteMaxRisk = "LOW";
        /** 标准自动执行最低证据强度（默认 0.7），用于 MEDIUM 等较高要求场景 */
        private double autoExecuteMinConfidence = 0.7;
        /**
         * LOW 风险止血动作最低证据强度（默认 0.55）。
         * 踩线 MQ 限流等「先自治、后人工」场景走此档，MEDIUM 仍须满足 {@link #autoExecuteMinConfidence} 且不超过 maxRisk。
         */
        private double autoExecuteMinConfidenceLow = 0.55;
        private double cacheHitRateWarnThreshold = 0.5;
        private long mqFailedWarnThreshold = 10;
        /** MQ 失败滑动窗口（分钟），自治检测只看窗口内失败次数，默认 5 分钟 */
        private long mqFailureWindowMinutes = 5;
        /**
         * 可选：显式指定 tenant，覆盖 {@code spring.application.name}。
         * 同一应用多环境共用 Redis 且需逻辑隔离时可配置；默认留空。
         */
        private String tenantId = "";
        private LedgerProperties ledger = new LedgerProperties();
        /** MQ 自治执行器参数（限流、延迟重试） */
        private MqActuatorProperties mq = new MqActuatorProperties();
        /** 编排器：多实例 tick 分布式锁等 */
        private OrchestratorProperties orchestrator = new OrchestratorProperties();
        /** 配置推荐采纳：audit-only 或 nacos-draft */
        private AdoptionProperties adoption = new AdoptionProperties();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getScanIntervalMs() {
            return scanIntervalMs;
        }

        public void setScanIntervalMs(long scanIntervalMs) {
            this.scanIntervalMs = scanIntervalMs;
        }

        public String getAutoExecuteMaxRisk() {
            return autoExecuteMaxRisk;
        }

        public void setAutoExecuteMaxRisk(String autoExecuteMaxRisk) {
            this.autoExecuteMaxRisk = autoExecuteMaxRisk;
        }

        public double getAutoExecuteMinConfidence() {
            return autoExecuteMinConfidence;
        }

        public void setAutoExecuteMinConfidence(double autoExecuteMinConfidence) {
            this.autoExecuteMinConfidence = autoExecuteMinConfidence;
        }

        public double getAutoExecuteMinConfidenceLow() {
            return autoExecuteMinConfidenceLow;
        }

        public void setAutoExecuteMinConfidenceLow(double autoExecuteMinConfidenceLow) {
            this.autoExecuteMinConfidenceLow = autoExecuteMinConfidenceLow;
        }

        public double getCacheHitRateWarnThreshold() {
            return cacheHitRateWarnThreshold;
        }

        public void setCacheHitRateWarnThreshold(double cacheHitRateWarnThreshold) {
            this.cacheHitRateWarnThreshold = cacheHitRateWarnThreshold;
        }

        public long getMqFailedWarnThreshold() {
            return mqFailedWarnThreshold;
        }

        public void setMqFailedWarnThreshold(long mqFailedWarnThreshold) {
            this.mqFailedWarnThreshold = mqFailedWarnThreshold;
        }

        public long getMqFailureWindowMinutes() {
            return mqFailureWindowMinutes;
        }

        public void setMqFailureWindowMinutes(long mqFailureWindowMinutes) {
            this.mqFailureWindowMinutes = mqFailureWindowMinutes;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public LedgerProperties getLedger() {
            return ledger;
        }

        public void setLedger(LedgerProperties ledger) {
            this.ledger = ledger;
        }

        public MqActuatorProperties getMq() {
            return mq;
        }

        public void setMq(MqActuatorProperties mq) {
            this.mq = mq;
        }

        public OrchestratorProperties getOrchestrator() {
            return orchestrator;
        }

        public void setOrchestrator(OrchestratorProperties orchestrator) {
            this.orchestrator = orchestrator;
        }

        public AdoptionProperties getAdoption() {
            return adoption;
        }

        public void setAdoption(AdoptionProperties adoption) {
            this.adoption = adoption;
        }
    }

    /**
     * 配置推荐采纳模式与 Nacos 草稿参数。
     *
     * <p>{@code mode=audit-only}（默认）仅写账本审计；
     * {@code nacos-draft} 采纳时生成 draft dataId，须控制台二次「确认发布」才写入生产。</p>
     */
    public static class AdoptionProperties {
        /** audit-only | nacos-draft */
        private String mode = "audit-only";
        /** Nacos draft / 生产配置的 group */
        private String draftGroup = "DEFAULT_GROUP";
        /** draft dataId 后缀，完整为 {@code {appName}{suffix}-{recommendationId}.yaml} */
        private String draftDataIdSuffix = "-autonomy-draft";
        /** 生产 dataId 后缀，完整为 {@code {appName}{suffix}}，默认 .yaml */
        private String productionDataIdSuffix = ".yaml";

        public boolean isNacosDraftMode() {
            return "nacos-draft".equalsIgnoreCase(mode);
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getDraftGroup() {
            return draftGroup;
        }

        public void setDraftGroup(String draftGroup) {
            this.draftGroup = draftGroup;
        }

        public String getDraftDataIdSuffix() {
            return draftDataIdSuffix;
        }

        public void setDraftDataIdSuffix(String draftDataIdSuffix) {
            this.draftDataIdSuffix = draftDataIdSuffix;
        }

        public String getProductionDataIdSuffix() {
            return productionDataIdSuffix;
        }

        public void setProductionDataIdSuffix(String productionDataIdSuffix) {
            this.productionDataIdSuffix = productionDataIdSuffix;
        }
    }

    /**
     * 自治编排器配置：多实例 tick 互斥等。
     *
     * <p>多 Pod 部署 order-system 时，将 {@link #distributedLockEnabled} 设为 true，
     * 并确保 {@code ledger.type=redisson}，账本与锁共用 Redis。</p>
     */
    public static class OrchestratorProperties {
        /**
         * 是否启用 tick 分布式锁。
         * false（默认）：本地/Demo 无感；true：同一 tenant 仅一个实例执行 tick。
         */
        private boolean distributedLockEnabled = false;
        /**
         * 锁租约（秒）。应大于单次 tick 最坏耗时；leader 正常结束时在 finally 主动 unlock。
         * leader 宕机时依赖租约过期，其它实例可在下一轮接管。
         */
        private long tickLockTtlSeconds = 30;

        public boolean isDistributedLockEnabled() {
            return distributedLockEnabled;
        }

        public void setDistributedLockEnabled(boolean distributedLockEnabled) {
            this.distributedLockEnabled = distributedLockEnabled;
        }

        public long getTickLockTtlSeconds() {
            return tickLockTtlSeconds;
        }

        public void setTickLockTtlSeconds(long tickLockTtlSeconds) {
            this.tickLockTtlSeconds = tickLockTtlSeconds;
        }
    }

    /**
     * MQ 自治执行器配置：消费限流与失败消息延迟重试批次。
     */
    public static class MqActuatorProperties {
        /** 限流窗口内允许的最大消费次数 */
        private int throttleLimit = 30;
        /** 限流时间窗口（秒） */
        private long throttleWindowSeconds = 60;
        /** 延迟重试单批次最大条数 */
        private int delayedRetryBatchSize = 10;
        /** 延迟重试投递延迟（毫秒） */
        private long delayedRetryDelayMs = 5000;

        public int getThrottleLimit() {
            return throttleLimit;
        }

        public void setThrottleLimit(int throttleLimit) {
            this.throttleLimit = throttleLimit;
        }

        public long getThrottleWindowSeconds() {
            return throttleWindowSeconds;
        }

        public void setThrottleWindowSeconds(long throttleWindowSeconds) {
            this.throttleWindowSeconds = throttleWindowSeconds;
        }

        public int getDelayedRetryBatchSize() {
            return delayedRetryBatchSize;
        }

        public void setDelayedRetryBatchSize(int delayedRetryBatchSize) {
            this.delayedRetryBatchSize = delayedRetryBatchSize;
        }

        public long getDelayedRetryDelayMs() {
            return delayedRetryDelayMs;
        }

        public void setDelayedRetryDelayMs(long delayedRetryDelayMs) {
            this.delayedRetryDelayMs = delayedRetryDelayMs;
        }
    }

    /**
     * 账本存储：memory（默认，重启丢失）| redisson（Phase 2）。
     */
    public static class LedgerProperties {
        private String type = "memory";
        private int maxRuns = 200;
        /** Redis key 前缀，仅 ledger.type=redisson 时生效 */
        private String keyPrefix = "ms:autonomy:run";
        /** run 记录在 Redis 中的保留时间（小时） */
        private long ttlHours = 168;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getMaxRuns() {
            return maxRuns;
        }

        public void setMaxRuns(int maxRuns) {
            this.maxRuns = maxRuns;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public long getTtlHours() {
            return ttlHours;
        }

        public void setTtlHours(long ttlHours) {
            this.ttlHours = ttlHours;
        }
    }

    /**
     * AI 控制台（问题列表、时间线、聊天入口）
     */
    public static class ConsoleProperties {

        private static final Logger log = LoggerFactory.getLogger(ConsoleProperties.class);

        private boolean enabled = false;
        private String basePath = "/ms-console";
        /** 是否启用 LLM 对话（false 时使用规则模式 Insight Tool） */
        private boolean llmEnabled = false;
        /** 旧配置项，兼容一个版本 */
        private boolean chatEnabledLegacy = false;
        private boolean chatEnabledLegacySet = false;
        private boolean llmEnabledExplicit = false;
        /**
         * 控制台 API 共享密钥；非空时启用鉴权（Header X-MS-Console-Token 或 ?token=）。
         * 本地 Demo 留空即可。
         */
        private String authToken = "";
        /** LLM 连接参数（OpenAI 兼容，默认 DeepSeek） */
        @NestedConfigurationProperty
        private LlmProperties llm = new LlmProperties();
        /** Phase 5.3 工作上下文（run 快照 + 压缩对话态 + 检索降级） */
        @NestedConfigurationProperty
        private ContextProperties context = new ContextProperties();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public boolean isLlmEnabled() {
            if (llmEnabledExplicit) {
                return llmEnabled;
            }
            if (chatEnabledLegacySet) {
                return chatEnabledLegacy;
            }
            return llmEnabled;
        }

        public void setLlmEnabled(boolean llmEnabled) {
            this.llmEnabled = llmEnabled;
            this.llmEnabledExplicit = true;
        }

        /**
         * @deprecated 请改用 {@code ms.middleware.console.llm-enabled}
         */
        @Deprecated
        public boolean isChatEnabled() {
            return isLlmEnabled();
        }

        /**
         * @deprecated 请改用 {@code ms.middleware.console.llm-enabled}
         */
        @Deprecated
        public void setChatEnabled(boolean chatEnabled) {
            log.warn("配置 ms.middleware.console.chat-enabled 已废弃，请改用 llm-enabled");
            this.chatEnabledLegacy = chatEnabled;
            this.chatEnabledLegacySet = true;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public LlmProperties getLlm() {
            return llm;
        }

        public void setLlm(LlmProperties llm) {
            this.llm = llm;
        }

        public ContextProperties getContext() {
            return context;
        }

        public void setContext(ContextProperties context) {
            this.context = context;
        }
    }

    /**
     * Phase 5.3 控制台工作上下文配置。
     */
    public static class ContextProperties {

        /** 是否启用工作上下文装配 */
        private boolean enabled = true;
        /** 工作上下文总字符预算 */
        private int maxChars = 2048;
        /** 保留最近几条用户原话（非完整 assistant 回复） */
        private int dialogUserMessages = 2;
        /** run 时间线尾部最多几条 */
        private int timelineEventLimit = 5;
        /** 战时诊断类意图注入失败 Trace 条数上限 */
        private int wartimeTraceLimit = 5;
        /** run 快照缓存秒数 */
        private int runContextCacheSeconds = 30;
        /** 仅 1 个活跃故障时：仅「跟进/诊断」类意图才自动绑定 runId（主页问指标/问题列表不绑） */
        private boolean autoBindSingleActiveRun = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxChars() {
            return maxChars;
        }

        public void setMaxChars(int maxChars) {
            this.maxChars = maxChars;
        }

        public int getDialogUserMessages() {
            return dialogUserMessages;
        }

        public void setDialogUserMessages(int dialogUserMessages) {
            this.dialogUserMessages = dialogUserMessages;
        }

        public int getTimelineEventLimit() {
            return timelineEventLimit;
        }

        public void setTimelineEventLimit(int timelineEventLimit) {
            this.timelineEventLimit = timelineEventLimit;
        }

        public int getWartimeTraceLimit() {
            return wartimeTraceLimit;
        }

        public void setWartimeTraceLimit(int wartimeTraceLimit) {
            this.wartimeTraceLimit = wartimeTraceLimit;
        }

        public int getRunContextCacheSeconds() {
            return runContextCacheSeconds;
        }

        public void setRunContextCacheSeconds(int runContextCacheSeconds) {
            this.runContextCacheSeconds = runContextCacheSeconds;
        }

        public boolean isAutoBindSingleActiveRun() {
            return autoBindSingleActiveRun;
        }

        public void setAutoBindSingleActiveRun(boolean autoBindSingleActiveRun) {
            this.autoBindSingleActiveRun = autoBindSingleActiveRun;
        }
    }

    /**
     * 控制台 LLM（OpenAI 兼容 API）。API Key 优先读配置，否则读环境变量 {@code MS_LLM_API_KEY}。
     */
    public static class LlmProperties {

        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "";
        private String model = "deepseek-chat";
        private double temperature = 0.2;
        private int timeoutSeconds = 60;
        /** Tool Grounding 模式：relaxed（默认）或 strict（运维类问题强制预调 Insight Tool） */
        private String groundingMode = "relaxed";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public String getGroundingMode() {
            return groundingMode;
        }

        public void setGroundingMode(String groundingMode) {
            this.groundingMode = groundingMode;
        }

        /** 解析有效 API Key：配置非空优先，否则 {@code MS_LLM_API_KEY}。 */
        public String resolveApiKey() {
            if (apiKey != null && !apiKey.isBlank()) {
                return apiKey.trim();
            }
            String env = System.getenv("MS_LLM_API_KEY");
            return env != null ? env.trim() : "";
        }
    }
}
