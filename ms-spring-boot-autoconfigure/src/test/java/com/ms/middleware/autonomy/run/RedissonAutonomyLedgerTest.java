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
        ledger = new RedissonAutonomyLedger(
                redissonClient,
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
