package com.blog.blog.common.util;

import com.blog.blog.common.constant.BlogCacheConstants;
import org.springframework.stereotype.Component;

/**
 * 缓存清理工具类
 * 集中管理缓存清理逻辑,避免 clearRelatedCaches 在多个类中重复定义
 */
@Component
public class CacheCleaner {

    private final RedisUtil redisUtil;

    public CacheCleaner(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * 清理文章相关缓存(增/删/改文章后调用)
     * 统一通过此方法清理,保证缓存 key 和清理逻辑一致
     */
    public void clearArticleRelatedCaches(Long articleId) {
        redisUtil.delete(BlogCacheConstants.ARTICLE_LIST_KEY);
        redisUtil.deleteByPattern(BlogCacheConstants.ARTICLE_LIST_PAGE_KEY + "*");
        redisUtil.deleteByPattern(BlogCacheConstants.ARTICLE_TOTAL_COUNT_KEY + "*");
        redisUtil.delete(BlogCacheConstants.ARTICLE_DETAIL_KEY + articleId);
        redisUtil.delete(BlogCacheConstants.ARTICLE_RANKING_VIEWS_KEY);
        redisUtil.delete(BlogCacheConstants.ARTICLE_RANKING_LATEST_KEY);
    }
}