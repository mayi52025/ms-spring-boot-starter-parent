package com.ms.middleware.rate;

import com.ms.middleware.metrics.MsMetrics;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的限流实现
 */
public class RedisRateLimiter implements RateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final RedissonClient redissonClient;
    private final MsMetrics metrics;

    public RedisRateLimiter(RedissonClient redissonClient, MsMetrics metrics) {
        this.redissonClient = redissonClient;
        this.metrics = metrics;
    }

    @Override
    public boolean tryAcquire(String key, int limit, long window, TimeUnit unit) {
        try {
            String redisKey = "rate:limiter:counter:" + key;
            RBucket<Long> bucket = redissonClient.getBucket(redisKey);
            
            Long count = bucket.get();
            if (count == null) {
                bucket.set(1L, window, unit);
                metrics.incrementRateLimitPassed();
                return true;
            }

            if (count < limit) {
                bucket.set(count + 1, window, unit);
                metrics.incrementRateLimitPassed();
                return true;
            }

            logger.debug("Rate limit exceeded for key: {}", key);
            metrics.incrementRateLimitRejected();
            return false;
        } catch (Exception e) {
            logger.error("Failed to acquire rate limit: {}", key, e);
            metrics.incrementFailureCount();
            return true; // 降级策略：允许请求通过
        }
    }

    @Override
    public boolean tryAcquireWithTokenBucket(String key, int limit, long window, TimeUnit unit, int capacity) {
        try {
            String redisKey = "rate:limiter:token:" + key;
            RBucket<TokenBucketState> bucket = redissonClient.getBucket(redisKey);
            
            TokenBucketState state = bucket.get();
            long now = System.currentTimeMillis();
            
            if (state == null) {
                // 初始化令牌桶
                state = new TokenBucketState();
                state.setLastRefillTime(now);
                state.setTokens(capacity - 1); // 消耗一个令牌
                bucket.set(state, window, unit);
                metrics.incrementRateLimitPassed();
                return true;
            }

            // 计算时间差，补充令牌
            long timeElapsed = now - state.getLastRefillTime();
            long tokensToAdd = (timeElapsed * limit) / unit.toMillis(window);
            
            if (tokensToAdd > 0) {
                state.setTokens(Math.min(state.getTokens() + tokensToAdd, capacity));
                state.setLastRefillTime(now);
            }

            // 尝试获取令牌
            if (state.getTokens() > 0) {
                state.setTokens(state.getTokens() - 1);
                bucket.set(state, window, unit);
                metrics.incrementRateLimitPassed();
                return true;
            }

            logger.debug("Token bucket rate limit exceeded for key: {}", key);
            metrics.incrementRateLimitRejected();
            return false;
        } catch (Exception e) {
            logger.error("Failed to acquire token bucket rate limit: {}", key, e);
            metrics.incrementFailureCount();
            return true; // 降级策略：允许请求通过
        }
    }

    @Override
    public boolean tryAcquireWithSlidingWindow(String key, int limit, long window, TimeUnit unit) {
        try {
            String redisKey = "rate:limiter:sliding:" + key;
            RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(redisKey);
            
            long now = System.currentTimeMillis();
            long windowMillis = unit.toMillis(window);
            long threshold = now - windowMillis;
            
            // 移除窗口外的元素
            sortedSet.removeRangeByScore(0, true, threshold, true);
            
            // 检查当前窗口内的请求数
            if (sortedSet.size() < limit) {
                // 添加当前请求
                sortedSet.add(now, String.valueOf(now));
                // 设置过期时间，避免内存泄漏
                sortedSet.expire(window, unit);
                metrics.incrementRateLimitPassed();
                return true;
            }

            logger.debug("Sliding window rate limit exceeded for key: {}", key);
            metrics.incrementRateLimitRejected();
            return false;
        } catch (Exception e) {
            logger.error("Failed to acquire sliding window rate limit: {}", key, e);
            metrics.incrementFailureCount();
            return true; // 降级策略：允许请求通过
        }
    }

    @Override
    public long getCurrentCount(String key) {
        try {
            String redisKey = "rate:limiter:counter:" + key;
            RBucket<Long> bucket = redissonClient.getBucket(redisKey);
            Long count = bucket.get();
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Failed to get current count: {}", key, e);
            return 0;
        }
    }

    @Override
    public boolean reset(String key) {
        try {
            String counterKey = "rate:limiter:counter:" + key;
            String tokenKey = "rate:limiter:token:" + key;
            String slidingKey = "rate:limiter:sliding:" + key;
            
            redissonClient.getBucket(counterKey).delete();
            redissonClient.getBucket(tokenKey).delete();
            redissonClient.getScoredSortedSet(slidingKey).delete();
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to reset rate limit: {}", key, e);
            return false;
        }
    }

    /**
     * 令牌桶状态
     */
    public static class TokenBucketState {
        private long lastRefillTime;
        private long tokens;

        public long getLastRefillTime() {
            return lastRefillTime;
        }

        public void setLastRefillTime(long lastRefillTime) {
            this.lastRefillTime = lastRefillTime;
        }

        public long getTokens() {
            return tokens;
        }

        public void setTokens(long tokens) {
            this.tokens = tokens;
        }
    }

}