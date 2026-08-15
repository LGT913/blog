package com.blog.article.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope   // Nacos 配置变更时重建本 Bean，重新解析 @Value
@Data
public class ArticleConfig {

    // 带默认值：Nacos 读不到也能启动，读到了用 Nacos 的值
    @Value("${ms.redis.prefix:ms:article}")
    private String prefix;
}