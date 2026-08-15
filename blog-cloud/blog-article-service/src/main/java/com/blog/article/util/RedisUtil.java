package com.blog.article.util;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate,
                     RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean setIfAbsent(String key, Object value, long timeout) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
    }

    public void deleteByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
        } catch (Exception e) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    public <T> T executeLua(DefaultRedisScript<T> script, String key, Object... args) {
        return redisTemplate.execute(script, Collections.singletonList(key), args);
    }

    public boolean tryLock(String key, String value, long timeout) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(key, value, timeout, TimeUnit.SECONDS));
    }

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    public boolean unlock(String key, String value) {
        Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), value);
        return Long.valueOf(1).equals(result);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long incrementBy(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Long sadd(String key, Object value) {
        return redisTemplate.opsForSet().add(key, value);
    }

    public Set<Object> smembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public Long srem(String key, Object value) {
        return redisTemplate.opsForSet().remove(key, value);
    }

    public Boolean del(String key) {
        return redisTemplate.delete(key);
    }

    public Double zIncrBy(String key, Object member, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    public Double zScore(String key, Object member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    public void zAdd(String key, Object member, double score) {
        redisTemplate.opsForZSet().add(key, member, score);
    }

    public Long zRem(String key, Object member) {
        return redisTemplate.opsForZSet().remove(key, member);
    }

    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    public void setWithLogicalExpire(String key, Object value, long logicalExpireSeconds) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", value);
        wrapper.put("expireTime", System.currentTimeMillis() + logicalExpireSeconds * 1000);
        redisTemplate.opsForValue().set(key, wrapper, logicalExpireSeconds + 60, TimeUnit.SECONDS);
    }

    public Object getWithLogicalExpire(String key) {
        Object cacheObj = redisTemplate.opsForValue().get(key);
        if (cacheObj == null) return null;
        if (cacheObj instanceof Map) {
            Map<?, ?> wrapper = (Map<?, ?>) cacheObj;
            Long expireTime = (Long) wrapper.get("expireTime");
            if (expireTime != null && System.currentTimeMillis() > expireTime) {
                return "LOGICAL_EXPIRED";
            }
            return wrapper.get("data");
        }
        return cacheObj;
    }

    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    public RLock getFairLock(String key) {
        return redissonClient.getFairLock(key);
    }

    public RReadWriteLock getReadWriteLock(String key) {
        return redissonClient.getReadWriteLock(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    public void hSet(String key, String field, Object value, long timeoutSeconds) {
        redisTemplate.opsForHash().put(key, field, value);
        redisTemplate.expire(key, timeoutSeconds, TimeUnit.SECONDS);
    }

    public Boolean sismember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    public Long lPush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    public void lTrim(String key, long start, long end) {
        redisTemplate.opsForList().trim(key, start, end);
    }

    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }
}