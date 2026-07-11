package com.ms.middleware.rate;

import com.ms.middleware.metrics.MsMetrics;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 Redis 的限流实现
 */
public class RedisRateLimiter implements RateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final AtomicReference<RedissonClient> redissonClientRef;
    private final MsMetrics metrics;

    public RedisRateLimiter(AtomicReference<RedissonClient> redissonClientRef, MsMetrics metrics) {
        this.redissonClientRef = redissonClientRef;
        this.metrics = metrics;
    }

    private RedissonClient client() {
        return redissonClientRef.get();
    }

    @Override
    public boolean tryAcquire(String key, int limit, long window, TimeUnit unit) {
        try {
            String redisKey = "rate:limiter:counter:" + key;

            String luaScript =
                "local current = redis.call('GET', KEYS[1])\n" +
                "if current == false then\n" +
                "    redis.call('SET', KEYS[1], 1, 'EX', ARGV[2])\n" +
                "    return 1\n" +
                "end\n" +
                "if tonumber(current) < tonumber(ARGV[1]) then\n" +
                "    local new_val = redis.call('INCR', KEYS[1])\n" +
                "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
                "    return 1\n" +
                "end\n" +
                "return 0";

            Long result = client().getScript().eval(
                RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Arrays.asList(redisKey),
                String.valueOf(limit),
                String.valueOf(unit.toSeconds(window))
            );

            if (result != null && result == 1) {
                metrics.incrementRateLimitPassed();
                return true;
            }

            logger.debug("Rate limit exceeded for key: {}", key);
            metrics.incrementRateLimitRejected();
            return false;
        } catch (Exception e) {
            logger.error("Failed to acquire rate limit: {}", key, e);
            metrics.incrementFailureCount();
            return true;
        }
    }

    @Override
    public boolean tryAcquireWithTokenBucket(String key, int limit, long window, TimeUnit unit, int capacity) {
        try {
            String redisKey = "rate:limiter:token:" + key;
            RBucket<TokenBucketState> bucket = client().getBucket(redisKey);

            TokenBucketState state = bucket.get();
            long now = System.currentTimeMillis();

            if (state == null) {
                state = new TokenBucketState();
                state.setLastRefillTime(now);
                state.setTokens(capacity - 1);
                bucket.set(state, window, unit);
                metrics.incrementRateLimitPassed();
                return true;
            }

            long timeElapsed = now - state.getLastRefillTime();
            long tokensToAdd = (timeElapsed * limit) / unit.toMillis(window);

            if (tokensToAdd > 0) {
                state.setTokens(Math.min(state.getTokens() + tokensToAdd, capacity));
                state.setLastRefillTime(now);
            }

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
            return true;
        }
    }

    @Override
    public boolean tryAcquireWithSlidingWindow(String key, int limit, long window, TimeUnit unit) {
        try {
            String redisKey = "rate:limiter:sliding:" + key;
            RScoredSortedSet<String> sortedSet = client().getScoredSortedSet(redisKey);

            long now = System.currentTimeMillis();
            long windowMillis = unit.toMillis(window);
            long threshold = now - windowMillis;

            sortedSet.removeRangeByScore(0, true, threshold, true);

            if (sortedSet.size() < limit) {
                sortedSet.add(now, String.valueOf(now));
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
            return true;
        }
    }

    @Override
    public long getCurrentCount(String key) {
        try {
            String redisKey = "rate:limiter:counter:" + key;
            RBucket<Long> bucket = client().getBucket(redisKey);
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

            client().getBucket(counterKey).delete();
            client().getBucket(tokenKey).delete();
            client().getScoredSortedSet(slidingKey).delete();

            return true;
        } catch (Exception e) {
            logger.error("Failed to reset rate limit: {}", key, e);
            return false;
        }
    }

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
