package com.blog.blog.common;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

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
}