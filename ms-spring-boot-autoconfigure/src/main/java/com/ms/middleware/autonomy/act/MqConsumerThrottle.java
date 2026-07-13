package com.ms.middleware.autonomy.act;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.rate.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * MQ 消费端限流器：自治动作 {@link com.ms.middleware.autonomy.AutonomyActionType#THROTTLE_CONSUMER} 的落地实现。
 *
 * <p>通过 Redis {@link RateLimiter} 在消费路径施加背压，避免 MQ 失败积压时拖垮下游。
 * {@link com.ms.middleware.mq.RabbitMessageQueue} 在处理消息前调用 {@link #awaitPermit()}。</p>
 */
public class MqConsumerThrottle {

    private static final Logger logger = LoggerFactory.getLogger(MqConsumerThrottle.class);

    /** Redis 限流键，全局消费节流 */
    public static final String RATE_LIMIT_KEY = "autonomy:mq:consumer";

    private final RateLimiter rateLimiter;
    private final MsMiddlewareProperties properties;

    /** 是否处于自治限流状态 */
    private volatile boolean enabled;
    /** 当前窗口内允许的最大消费次数 */
    private volatile int limit;
    /** 限流时间窗口（秒） */
    private volatile long windowSeconds;

    public MqConsumerThrottle(RateLimiter rateLimiter, MsMiddlewareProperties properties) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    /**
     * 启用消费限流（由自治执行器在 MQ_DEGRADED 场景调用）。
     *
     * @param limit         窗口内最大消费次数；≤0 时使用配置默认值
     * @param windowSeconds 窗口秒数；≤0 时使用配置默认值
     * @return 是否启用成功
     */
    public boolean enable(int limit, long windowSeconds) {
        MsMiddlewareProperties.MqActuatorProperties mq = properties.getAutonomy().getMq();
        this.limit = limit > 0 ? limit : mq.getThrottleLimit();
        this.windowSeconds = windowSeconds > 0 ? windowSeconds : mq.getThrottleWindowSeconds();
        rateLimiter.reset(RATE_LIMIT_KEY);
        this.enabled = true;
        logger.info("MQ 消费限流已启用: limit={}/{}s", this.limit, this.windowSeconds);
        return true;
    }

    /** 关闭限流并清理 Redis 计数（STABLE 或人工恢复时调用） */
    public void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        rateLimiter.reset(RATE_LIMIT_KEY);
        logger.info("MQ 消费限流已关闭");
    }

    /** 是否处于限流状态 */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 消费前获取许可；未启用时直接放行。
     * 超限时短暂自旋等待，对消费线程施加背压。
     */
    public void awaitPermit() throws InterruptedException {
        if (!enabled) {
            return;
        }
        int spins = 0;
        while (!rateLimiter.tryAcquireWithTokenBucket(
                RATE_LIMIT_KEY, limit, windowSeconds, TimeUnit.SECONDS, limit)) {
            Thread.sleep(50);
            spins++;
            if (spins > 200) {
                // 最多等待约 10s，避免无限阻塞
                break;
            }
        }
    }

    public int getLimit() {
        return limit;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }
}
