package com.blog.blog.repository;

import com.blog.blog.entity.SiteConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteConfigRepository extends JpaRepository<SiteConfig, Long> {

    // 根据配置键查询单条数据
    Optional<SiteConfig> findByConfigKey(String configKey);
}
