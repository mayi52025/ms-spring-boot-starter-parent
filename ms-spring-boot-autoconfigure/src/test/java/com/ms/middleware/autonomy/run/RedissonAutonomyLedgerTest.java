package com.ms.middleware.autonomy.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.autonomy.AutonomyRunStatus;
import com.ms.middleware.autonomy.tenant.AutonomyTenantProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.ms.middleware.redis.RedissonConnectionManager;
import org.redisson.config.Config;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class RedissonAutonomyLedgerTest {

    private static final String TENANT = "order-system";

    private final Map<String, String> buckets = new ConcurrentHashMap<>();
    private final Map<String, TreeMap<Double, String>> indexes = new ConcurrentHashMap<>();
    private final List<Object> publishedEvents = new ArrayList<>();

    private RedissonAutonomyLedger ledger;

    @BeforeEach
    void setUp() {
        RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
        when(redissonClient.getBucket(anyString())).thenAnswer(inv -> mockBucket(inv.getArgument(0)));
        when(redissonClient.getScoredSortedSet(anyString())).thenAnswer(inv -> mockIndex(inv.getArgument(0)));

        AutonomyTenantProvider tenantProvider = () -> TENANT;
        // 单测使用 Mockito 假 RedissonClient，不会真连 Redis；地址仅占位，与 192.168.100.102 无关
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonConnectionManager connectionManager =
                new RedissonConnectionManager(new AtomicReference<>(redissonClient), config);
        ledger = new RedissonAutonomyLedger(
                connectionManager,
                new ObjectMapper().findAndRegisterModules(),
                publishedEvents::add,
                tenantProvider,
                "ms:autonomy:run",
                10,
                24);
    }

    @Test
    void startRunPersistsAndLists() {
        AutonomyRun run = newRun("r1");
        ledger.startRun(run);

        assertTrue(ledger.get("r1").isPresent());
        assertEquals(1, ledger.listRecent(5).size());
        assertEquals(TENANT, ledger.get("r1").orElseThrow().getTenant());
    }

    @Test
    void appendTimelineUpdatesStoredRun() {
        AutonomyRun run = newRun("r2");
        ledger.startRun(run);
        ledger.appendTimeline(run, "PLAN", "计划已生成");

        AutonomyRun loaded = ledger.get("r2").orElseThrow();
        assertEquals(2, loaded.getTimeline().size());
        assertEquals("PLAN", loaded.getTimeline().get(1).getPhase());
    }

    @Test
    void listActiveExcludesStable() {
        AutonomyRun active = newRun("active");
        ledger.startRun(active);

        AutonomyRun stable = newRun("stable");
        stable.setStatus(AutonomyRunStatus.STABLE);
        ledger.startRun(stable);

        assertEquals(1, ledger.listActive().size());
        assertEquals("active", ledger.listActive().get(0).getRunId());
    }

    /** Redisson 账本：不同 tenant 使用不同 Redis index key，列表互不可见 */
    @Test
    void crossTenantIsolation() {
        AtomicReference<String> tenantRef = new AtomicReference<>(TENANT);
        RedissonAutonomyLedger scopedLedger = createLedger(tenantRef::get);

        tenantRef.set("order-system");
        scopedLedger.startRun(newRun("run-a"));

        tenantRef.set("payment-service");
        scopedLedger.startRun(newRun("run-b"));
        assertEquals(1, scopedLedger.listRecent(10).size());
        assertTrue(scopedLedger.get("run-b").isPresent());
        assertTrue(scopedLedger.get("run-a").isEmpty());
    }

    private RedissonAutonomyLedger createLedger(AutonomyTenantProvider tenantProvider) {
        RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
        when(redissonClient.getBucket(anyString())).thenAnswer(inv -> mockBucket(inv.getArgument(0)));
        when(redissonClient.getScoredSortedSet(anyString())).thenAnswer(inv -> mockIndex(inv.getArgument(0)));
        // 占位 Config，不发起真实连接
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonConnectionManager connectionManager =
                new RedissonConnectionManager(new AtomicReference<>(redissonClient), config);
        return new RedissonAutonomyLedger(
                connectionManager,
                new ObjectMapper().findAndRegisterModules(),
                publishedEvents::add,
                tenantProvider,
                "ms:autonomy:run",
                10,
                24);
    }

    @SuppressWarnings("unchecked")
    private RBucket<String> mockBucket(String key) {
        RBucket<String> bucket = Mockito.mock(RBucket.class);
        when(bucket.get()).thenAnswer(inv -> buckets.get(key));
        doAnswer(inv -> {
            buckets.put(key, inv.getArgument(0));
            return null;
        }).when(bucket).set(ArgumentMatchers.<String>any(), ArgumentMatchers.any(Duration.class));
        when(bucket.delete()).thenAnswer(inv -> buckets.remove(key) != null);
        return bucket;
    }

    @SuppressWarnings("unchecked")
    private RScoredSortedSet<String> mockIndex(String key) {
        RScoredSortedSet<String> set = Mockito.mock(RScoredSortedSet.class);
        TreeMap<Double, String> index = indexes.computeIfAbsent(key, k -> new TreeMap<>());

        when(set.add(anyDouble(), anyString())).thenAnswer(inv -> {
            index.put(inv.getArgument(0), inv.getArgument(1));
            return true;
        });
        when(set.size()).thenAnswer(inv -> index.size());
        when(set.remove(anyString())).thenAnswer(inv -> index.values().remove(inv.getArgument(0)));
        when(set.valueRange(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenAnswer(inv -> {
            List<String> values = index.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .toList();
            int from = inv.getArgument(0);
            int to = inv.getArgument(1);
            if (from >= values.size()) {
                return List.of();
            }
            return values.subList(from, Math.min(to + 1, values.size()));
        });
        when(set.valueRangeReversed(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenAnswer(inv -> {
            List<String> values = index.entrySet().stream()
                    .sorted(Map.Entry.<Double, String>comparingByKey().reversed())
                    .map(Map.Entry::getValue)
                    .toList();
            int from = inv.getArgument(0);
            int to = inv.getArgument(1);
            if (from >= values.size()) {
                return List.of();
            }
            return values.subList(from, Math.min(to + 1, values.size()));
        });
        return set;
    }

    private static AutonomyRun newRun(String runId) {
        AutonomyRun run = new AutonomyRun();
        run.setRunId(runId);
        run.setTenant(TENANT);
        return run;
    }
}
