package com.ms.middleware.autonomy.orchestrator;

import com.ms.middleware.redis.RedissonConnectionManager;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 Redisson {@link RLock} 的 tick 分布式锁实现。
 *
 * <p><b>为什么用 Redisson：</b>项目 Phase 2 已用 Redisson 做自治账本（{@code ledger.type=redisson}），
 * 复用同一 Redis 连接与 {@link RedissonConnectionManager} 自愈能力，不引入新中间件。</p>
 *
 * <p><b>锁 key：</b>{@code ms:autonomy:tick:{tenant}}，与账本 key {@code ms:autonomy:run:*} 分离，
 * 避免与 run 数据混淆；tenant 来自 {@code spring.application.name}，多应用同 Redis 时互不抢锁。</p>
 *
 * <p><b>为什么 {@code tryLock(0, ttl, SECONDS)}：</b></p>
 * <ul>
 *   <li>waitTime=0 — 不阻塞调度线程；抢不到锁说明其它实例已是 leader，本实例跳过即可</li>
 *   <li>leaseTime=ttl — 防止 leader 宕机后锁永不释放（看门狗由 Redisson 在持锁期间续期）</li>
 * </ul>
 */
public class RedissonAutonomyTickLock implements AutonomyTickLock {

    private static final Logger logger = LoggerFactory.getLogger(RedissonAutonomyTickLock.class);

    /** 与账本前缀独立，专用于 tick 互斥 */
    static final String KEY_PREFIX = "ms:autonomy:tick:";

    /** 跟随 RedissonConnectionManager 切换，Redis 恢复后自动可用 */
    private final AtomicReference<RedissonClient> redissonClientRef;
    /** 锁租约秒数，应 ≥ 单次 tick 最坏耗时（配置项 tick-lock-ttl-seconds） */
    private final long tickLockTtlSeconds;

    public RedissonAutonomyTickLock(RedissonConnectionManager connectionManager, long tickLockTtlSeconds) {
        this.redissonClientRef = connectionManager.getClientRef();
        this.tickLockTtlSeconds = Math.max(1, tickLockTtlSeconds);
    }

    @Override
    public void runIfLeader(String tenant, Runnable action) {
        RedissonClient client = redissonClientRef.get();
        if (client == null) {
            // Redis 不可用时不强行走 tick，避免多实例在降级态争抢本地状态
            logger.debug("Redisson 不可用，跳过 tick tenant={}", tenant);
            return;
        }
        String lockKey = KEY_PREFIX + normalizeTenant(tenant);
        RLock lock = client.getLock(lockKey);
        boolean acquired = false;
        try {
            // 非阻塞抢锁：leader 执行，follower 本轮直接 return
            acquired = lock.tryLock(0, tickLockTtlSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                logger.debug("未获得 tick 分布式锁，跳过本轮 tenant={} key={}", tenant, lockKey);
                return;
            }
            action.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("获取 tick 锁被中断 tenant={}", tenant);
        } finally {
            // 仅当前线程持锁时才 unlock，避免误释其他实例的锁
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 空 tenant 统一落到 default，防止 key 中出现空白段 */
    static String normalizeTenant(String tenant) {
        if (tenant == null || tenant.isBlank()) {
            return "default";
        }
        return tenant.trim();
    }
}
