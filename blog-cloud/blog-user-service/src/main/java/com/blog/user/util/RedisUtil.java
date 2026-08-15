package com.blog.user.util;

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
     //存入缓存（无过期时间）

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

     //存入缓存并设置过期时间
     //@param timeout 过期时间（单位：秒）
    public void set(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

     //从缓存取值
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    //删除缓存
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    //判断 key 是否存在
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    //分布式锁：setIfAbsent（不存在才写入），返回是否加锁成功
    public Boolean setIfAbsent(String key, Object value, long timeout) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
    }

    //模糊删除：用 SCAN 迭代器遍历匹配的 key 并删除（避免 KEYS 阻塞 Redis）
    public void deleteByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
        } catch (Exception e) {
            // SCAN 失败时兜底:退化到 KEYS(只记警告,不阻塞主流程)
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    //执行 Lua 脚本（用于安全释放分布式锁）
    public <T> T executeLua(DefaultRedisScript<T> script, String key, Object... args) {
        return redisTemplate.execute(script, Collections.singletonList(key), args);
    }

    //新增setIfAbsent 和 executeLua，tryLock 和 unlock 对这些能力的封装
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

    // 原子递增（阅读量 +1），返回递增后的值
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    // 原子递增指定数量（支持负数递减），返回递增后的值
    public Long incrementBy(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    // 原子读取并归零（同步时用，防止并发丢失）
    public Long getAndReset(String key) {
        Object val = redisTemplate.opsForValue().getAndSet(key, 0);
        if (val == null) {
            return null;
        }
        if (val instanceof Long) {
            return (Long) val;
        }
        if (val instanceof Integer) {
            return ((Integer) val).longValue();
        }
        try {
            return Long.valueOf(val.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    // 向集合添加元素（标记脏文章）
    public Long sadd(String key, Object value) {
        return redisTemplate.opsForSet().add(key, value);
    }

    // 获取集合所有元素（获取脏文章 ID 列表）
    public Set<Object> smembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    // 从集合中移除元素（替代直接 del,避免丢失同步过程中新增的脏标记）
    public Long srem(String key, Object value) {
        return redisTemplate.opsForSet().remove(key, value);
    }

    // 删除集合
    public Boolean del(String key) {
        return redisTemplate.delete(key);
    }

    // ZSet:原子递增阅读量(ZINCRBY)
    public Double zIncrBy(String key, Object member, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

    // ZSet:获取 Top N(按 score 降序)
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    // ZSet:获取 member 的 score
    public Double zScore(String key, Object member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    // ZSet:初始化(批量添加)
    public void zAdd(String key, Object member, double score) {
        redisTemplate.opsForZSet().add(key, member, score);
    }

    // ZSet:删除 member
    public Long zRem(String key, Object member) {
        return redisTemplate.opsForZSet().remove(key, member);
    }

    // ZSet:获取集合大小(ZCard)
    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    // 逻辑过期:写入时带 expireTime,物理过期设为 expireTime + 缓冲
    public void setWithLogicalExpire(String key, Object value, long logicalExpireSeconds) {
        // 包装成 LogicalExpireData
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", value);
        wrapper.put("expireTime", System.currentTimeMillis() + logicalExpireSeconds * 1000);
        // 物理过期 = 逻辑过期 + 60 秒缓冲(兜底,防止逻辑过期失效)
        redisTemplate.opsForValue().set(key, wrapper, logicalExpireSeconds + 60, TimeUnit.SECONDS);
    }

    // 逻辑过期:读取时判断是否过期
    public Object getWithLogicalExpire(String key) {
        Object cacheObj = redisTemplate.opsForValue().get(key);
        if (cacheObj == null) return null;

        if (cacheObj instanceof Map) {
            Map<?, ?> wrapper = (Map<?, ?>) cacheObj;
            Long expireTime = (Long) wrapper.get("expireTime");
            if (expireTime != null && System.currentTimeMillis() > expireTime) {
                // 逻辑过期
                return "LOGICAL_EXPIRED";  // 特殊标记
            }
            return wrapper.get("data");
        }
        return cacheObj;  // 旧格式,直接返回
    }



    // 获取可重入互斥锁(带看门狗,自动续期)
    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    // 获取公平锁(按等待顺序获取)
    public RLock getFairLock(String key) {
        return redissonClient.getFairLock(key);
    }

    // 获取读写锁(读多写少场景)
    public RReadWriteLock getReadWriteLock(String key) {
        return redissonClient.getReadWriteLock(key);
    }

    // 通用过期设置
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    // Hash 操作
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    public void hSet(String key, String field, Object value, long timeoutSeconds) {
        redisTemplate.opsForHash().put(key, field, value);
        redisTemplate.expire(key, timeoutSeconds, TimeUnit.SECONDS);
    }

    //Set 操作  判断元素是否在集合中
    public Boolean sismember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    //List 操作（浏览记录用） 从左侧推入元素
    public Long lPush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    // 修剪列表，只保留指定范围内的元素
    public void lTrim(String key, long start, long end) {
        redisTemplate.opsForList().trim(key, start, end);
    }

    // 获取列表指定范围内的元素
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }
}