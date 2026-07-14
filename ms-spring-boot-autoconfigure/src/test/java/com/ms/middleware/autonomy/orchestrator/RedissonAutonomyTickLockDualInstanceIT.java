package com.ms.middleware.autonomy.orchestrator;

import com.ms.middleware.redis.RedissonConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 双 JVM 模拟：两个独立 Redisson 客户端连接同一 Redis，验证 tick 锁互斥。
 *
 * <p>等价于两个 order-system 实例共用 {@code 192.168.100.102:6379} 时，
 * 每轮 tick 仅一个实例成为 leader 执行 {@code doTick()}。</p>
 */
class RedissonAutonomyTickLockDualInstanceIT {

    private static final String REDIS_HOST = "192.168.100.102";
    private static final int REDIS_PORT = 6379;
    /** 专用 tenant，避免与正在运行的 order-system 抢锁 */
    private static final String TENANT = "dual-instance-it";

    private final List<RedissonClient> clients = new ArrayList<>();

    @BeforeEach
    void assumeRedisReachable() {
        assumeTrue(isRedisReachable(), () -> "跳过：无法连接 Redis " + REDIS_HOST + ":" + REDIS_PORT);
    }

    @AfterEach
    void shutdownClients() {
        for (RedissonClient client : clients) {
            if (client != null && !client.isShutdown()) {
                client.shutdown();
            }
        }
        clients.clear();
    }

    /**
     * 每轮两个「实例」同时 tick，20 轮内每轮恰好 1 次执行。
     */
    @Test
    void twoInstancesOnlyOneLeaderPerTickRound() throws Exception {
        RedissonAutonomyTickLock lockA = newLockInstance();
        RedissonAutonomyTickLock lockB = newLockInstance();
        AtomicInteger executed = new AtomicInteger(0);
        Runnable tickBody = executed::incrementAndGet;

        int rounds = 20;
        for (int round = 0; round < rounds; round++) {
            executed.set(0);
            CountDownLatch start = new CountDownLatch(1);
            Thread instanceA = new Thread(() -> {
                awaitStart(start);
                lockA.runIfLeader(TENANT, tickBody);
            });
            Thread instanceB = new Thread(() -> {
                awaitStart(start);
                lockB.runIfLeader(TENANT, tickBody);
            });
            instanceA.start();
            instanceB.start();
            start.countDown();
            instanceA.join(5000);
            instanceB.join(5000);
            assertEquals(1, executed.get(), "第 " + round + " 轮应仅一个 leader 执行 tick");
        }
    }

    /**
     * 8 个并发 worker 模拟多 Pod，每批仅 1 个获锁。
     */
    @Test
    void manyConcurrentWorkersOnlyOneLeader() throws Exception {
        int workers = 8;
        RedissonAutonomyTickLock[] locks = new RedissonAutonomyTickLock[workers];
        for (int i = 0; i < workers; i++) {
            locks[i] = newLockInstance();
        }

        AtomicInteger executed = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try {
            for (int round = 0; round < 10; round++) {
                executed.set(0);
                CountDownLatch start = new CountDownLatch(1);
                CountDownLatch done = new CountDownLatch(workers);
                for (RedissonAutonomyTickLock lock : locks) {
                    pool.submit(() -> {
                        awaitStart(start);
                        lock.runIfLeader(TENANT, executed::incrementAndGet);
                        done.countDown();
                    });
                }
                start.countDown();
                assumeTrue(done.await(10, TimeUnit.SECONDS), "worker 超时");
                assertEquals(1, executed.get(), "并发 " + workers + " worker 每轮应仅 1 次 tick");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private RedissonAutonomyTickLock newLockInstance() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + REDIS_HOST + ":" + REDIS_PORT)
                .setDatabase(0)
                .setConnectTimeout(3000)
                .setTimeout(3000);
        RedissonClient client = org.redisson.Redisson.create(config);
        clients.add(client);
        RedissonConnectionManager manager = new RedissonConnectionManager(new AtomicReference<>(client), config);
        return new RedissonAutonomyTickLock(manager, 30);
    }

    private static void awaitStart(CountDownLatch start) {
        try {
            start.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isRedisReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(REDIS_HOST, REDIS_PORT), 3000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
