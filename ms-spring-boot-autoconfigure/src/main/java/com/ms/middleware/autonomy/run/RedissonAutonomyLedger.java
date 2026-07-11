package com.ms.middleware.autonomy.run;

import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 Redisson 的自治账本：run 重启不丢失，按 tenant 隔离。
 *
 * <p>Redis 结构：</p>
 * <ul>
 *   <li>{@code {keyPrefix}{tenant}:{runId}} — RBucket，JSON 存 AutonomyRun</li>
 *   <li>{@code {indexPrefix}{tenant}} — RScoredSortedSet，score=startedAt，member=runId</li>
 * </ul>
 * <p>Redis 不可用时降级到进程内缓存；恢复后通过 {@link AtomicReference} 跟随自愈切换连接。</p>
 */
public class RedissonAutonomyLedger extends AbstractAutonomyLedger {

    private static final Logger logger = LoggerFactory.getLogger(RedissonAutonomyLedger.class);

    /** Redis 宕机时的进程内降级存储，避免「账本在 Redis 上却检测不到 Redis 故障」的死锁 */
    private final Map<String, AutonomyRun> localFallback = new ConcurrentHashMap<>();

    private final AtomicReference<RedissonClient> redissonClientRef;
    private final AutonomyRunSerde serde;
    private final String keyPrefix;
    private final String indexPrefix;
    private final int maxRuns;
    private final Duration ttl;

    public RedissonAutonomyLedger(AtomicReference<RedissonClient> redissonClientRef,
                                  ObjectMapper objectMapper,
                                  ApplicationEventPublisher eventPublisher,
                                  AutonomyTenantProvider tenantProvider,
                                  String keyPrefix,
                                  int maxRuns,
                                  long ttlHours) {
        super(eventPublisher, tenantProvider);
        this.redissonClientRef = redissonClientRef;
        this.serde = new AutonomyRunSerde(objectMapper);
        this.keyPrefix = normalizePrefix(keyPrefix);
        this.indexPrefix = this.keyPrefix + "index:";
        this.maxRuns = maxRuns;
        this.ttl = Duration.ofHours(Math.max(1, ttlHours));
    }

    @Override
    public AutonomyRun startRun(AutonomyRun run) {
        ensureTenant(run);
        publishTimeline(run, "DETECT", "检测到中间件异常，runId=" + run.getRunId());
        cacheLocally(run);
        persistToRedis(run);
        return run;
    }

    @Override
    public Optional<AutonomyRun> get(String runId) {
        String tenant = currentTenant();
        AutonomyRun local = localFallback.get(storageKey(tenant, runId));
        AutonomyRun remote = loadRun(tenant, runId);
        if (local == null) {
            return Optional.ofNullable(remote);
        }
        if (remote == null) {
            return Optional.of(local);
        }
        return Optional.of(pickNewerRun(local, remote));
    }

    @Override
    public List<AutonomyRun> listRecent() {
        return listRecent(maxRuns);
    }

    @Override
    public List<AutonomyRun> listRecent(int limit) {
        String tenant = currentTenant();
        Map<String, AutonomyRun> merged = new LinkedHashMap<>();
        try {
            RedissonClient client = currentClient();
            if (client != null) {
                RScoredSortedSet<String> index = client.getScoredSortedSet(indexPrefix + tenant);
                Collection<String> runIds = index.valueRangeReversed(0, Math.max(0, limit - 1));
                for (String runId : runIds) {
                    AutonomyRun run = loadRun(tenant, runId);
                    if (run != null) {
                        mergeRun(merged, run);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to list recent runs from Redis for tenant {}", tenant, e);
        }
        localFallback.forEach((key, run) -> {
            if (tenant.equals(run.getTenant())) {
                mergeRun(merged, run);
            }
        });
        return merged.values().stream()
                .sorted(Comparator.comparing(AutonomyRun::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<AutonomyRun> listActive() {
        List<AutonomyRun> active = new ArrayList<>();
        for (AutonomyRun run : listRecent(maxRuns)) {
            AutonomyRunStatus status = run.getStatus();
            if (status != AutonomyRunStatus.STABLE && status != AutonomyRunStatus.CLOSED) {
                active.add(run);
            }
        }
        return active;
    }

    @Override
    public void appendTimeline(AutonomyRun run, String phase, String message) {
        publishTimeline(run, phase, message);
        saveRun(run);
    }

    @Override
    public void update(AutonomyRun run) {
        ensureTenant(run);
        saveRun(run);
    }

    private void saveRun(AutonomyRun run) {
        ensureTenant(run);
        cacheLocally(run);
        try {
            RedissonClient client = currentClient();
            if (client == null) {
                return;
            }
            String key = runKey(run.getTenant(), run.getRunId());
            RBucket<String> bucket = client.getBucket(key);
            bucket.set(serde.toJson(run), ttl);
        } catch (Exception e) {
            logger.warn("Failed to save autonomy run {} to Redis, kept in local fallback", run.getRunId(), e);
        }
    }

    private void persistToRedis(AutonomyRun run) {
        try {
            trimIfNeeded(run.getTenant());
            indexRun(run);
            saveRun(run);
        } catch (Exception e) {
            logger.warn("Redis ledger persist failed for run {}, using local fallback only", run.getRunId(), e);
        }
    }

    private void cacheLocally(AutonomyRun run) {
        ensureTenant(run);
        localFallback.put(storageKey(run.getTenant(), run.getRunId()), run);
    }

    private void mergeRun(Map<String, AutonomyRun> merged, AutonomyRun run) {
        merged.merge(run.getRunId(), run, this::pickNewerRun);
    }

    private AutonomyRun pickNewerRun(AutonomyRun left, AutonomyRun right) {
        Instant leftAt = left.getUpdatedAt();
        Instant rightAt = right.getUpdatedAt();
        if (leftAt == null) {
            return right;
        }
        if (rightAt == null) {
            return left;
        }
        return leftAt.isAfter(rightAt) ? left : right;
    }

    private RedissonClient currentClient() {
        RedissonClient client = redissonClientRef.get();
        if (client == null || client.isShutdown()) {
            return null;
        }
        return client;
    }

    private String storageKey(String tenant, String runId) {
        return tenant + ":" + runId;
    }

    private AutonomyRun loadRun(String tenant, String runId) {
        try {
            RedissonClient client = currentClient();
            if (client == null) {
                return null;
            }
            RBucket<String> bucket = client.getBucket(runKey(tenant, runId));
            return serde.fromJson(bucket.get());
        } catch (Exception e) {
            logger.warn("Failed to load autonomy run {} for tenant {}", runId, tenant, e);
            return null;
        }
    }

    private void indexRun(AutonomyRun run) {
        RedissonClient client = currentClient();
        if (client == null) {
            return;
        }
        double score = run.getStartedAt().toEpochMilli();
        client.getScoredSortedSet(indexPrefix + run.getTenant()).add(score, run.getRunId());
    }

    private void trimIfNeeded(String tenant) {
        RedissonClient client = currentClient();
        if (client == null) {
            return;
        }
        RScoredSortedSet<String> index = client.getScoredSortedSet(indexPrefix + tenant);
        int size = index.size();
        if (size < maxRuns) {
            return;
        }
        int removeCount = size - maxRuns + 1;
        Collection<String> oldest = index.valueRange(0, removeCount - 1);
        for (String runId : oldest) {
            client.getBucket(runKey(tenant, runId)).delete();
            index.remove(runId);
        }
    }

    private String runKey(String tenant, String runId) {
        return keyPrefix + tenant + ":" + runId;
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "ms:autonomy:run:";
        }
        return prefix.endsWith(":") ? prefix : prefix + ":";
    }
}
