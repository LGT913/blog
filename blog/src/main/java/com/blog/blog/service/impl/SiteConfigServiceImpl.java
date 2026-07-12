package com.blog.blog.service.impl;

import com.blog.blog.common.RedisUtil;
import com.blog.blog.entity.SiteConfig;
import com.blog.blog.repository.SiteConfigRepository;
import com.blog.blog.service.SiteConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;

@Service
public class SiteConfigServiceImpl implements SiteConfigService {

    @Autowired
    private SiteConfigRepository siteConfigRepository;

    @Autowired
    private RedisUtil redisUtil;

    // Redis key 常量
    private static final String SITE_CONFIG_KEY = "site:config";
    private static final long SITE_CONFIG_EXPIRE = 600L;

    @Override
    public SiteConfig getByConfigKey(String configKey) {
        String key = SITE_CONFIG_KEY;

        // 1. 先从 Redis 查
        Object cacheObj = redisUtil.get(key);
        if (!ObjectUtils.isEmpty(cacheObj)) {
            return (SiteConfig) cacheObj;
        }

        // 2. 缓存未命中，从数据库查
        SiteConfig siteConfig = siteConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new RuntimeException("网站配置不存在"));

        // 3. 写入 Redis，过期时间 10 分钟
        redisUtil.set(key, siteConfig, SITE_CONFIG_EXPIRE);
        return siteConfig;
    }

    @Override
    public SiteConfig saveConfig(String configKey, String configValue, String description) {
        // 先尝试查找已有配置，存在则更新，不存在则新建
        SiteConfig siteConfig = siteConfigRepository.findByConfigKey(configKey).orElse(null);

        if (siteConfig == null) {
            // 新建
            siteConfig = new SiteConfig();
            siteConfig.setConfigKey(configKey);
            siteConfig.setCreateTime(LocalDateTime.now());
        }

        siteConfig.setConfigValue(configValue);
        siteConfig.setDescription(description);
        siteConfig.setUpdateTime(LocalDateTime.now());
        siteConfig = siteConfigRepository.save(siteConfig);

        // 增删改数据 → 只删除缓存，不主动写入缓存
        redisUtil.delete(SITE_CONFIG_KEY);

        return siteConfig;
    }

    @Override
    public void deleteConfig(Long id) {
        siteConfigRepository.deleteById(id);

        // 删除数据 → 清理对应缓存
        redisUtil.delete(SITE_CONFIG_KEY);
    }
}
