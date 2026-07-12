package com.blog.blog.service;

import com.blog.blog.entity.SiteConfig;

public interface SiteConfigService {

    // 根据配置键查询单条数据（走缓存）
    SiteConfig getByConfigKey(String configKey);

    // 新增或更新配置（更新后删除缓存）
    SiteConfig saveConfig(String configKey, String configValue, String description);

    // 根据ID删除配置（删除后清理缓存）
    void deleteConfig(Long id);
}
