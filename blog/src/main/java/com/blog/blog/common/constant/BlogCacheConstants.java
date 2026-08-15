package com.blog.blog.common.constant;

/**
 * Redis 缓存 key 常量集中管理
 * 所有涉及缓存 key 的地方统一引用此类,避免重复定义导致 key 不一致
 */
public final class BlogCacheConstants {

    private BlogCacheConstants() {}

    //文章阅读量
    public static final String ARTICLE_VIEW_KEY = "blog:article:view:";
    public static final String ARTICLE_VIEW_DIRTY_KEY = "blog:article:view:dirty";

    //文章列表/分页
    public static final String ARTICLE_LIST_KEY = "blog:article:list";
    public static final String ARTICLE_LIST_PAGE_KEY = "blog:article:list:page:";
    public static final String ARTICLE_TOTAL_COUNT_KEY = "blog:article:total:count";

    //文章详情
    public static final String ARTICLE_DETAIL_KEY = "blog:article:detail:";
    public static final long ARTICLE_LIST_EXPIRE = 600L;
    public static final long ARTICLE_DETAIL_EXPIRE = 1800L;
    public static final long ARTICLE_RANKING_EXPIRE = 300L;
    public static final long ARTICLE_TOTAL_EXPIRE = 600L;
    public static final long NULL_CACHE_EXPIRE = 60L;

    //排行榜
    public static final String ARTICLE_RANKING_VIEWS_KEY = "blog:article:ranking:views";
    public static final String ARTICLE_RANKING_LATEST_KEY = "blog:article:ranking:latest";
    public static final String ARTICLE_RANKING_ZSET_VIEWS = "blog:article:ranking:zset:views";

    //分布式锁
    public static final String ARTICLE_LOCK_KEY = "blog:article:lock:";
    public static final String REBUILD_LOCK_PREFIX = "blog:cache:rebuild:lock:";
    public static final String ARTICLE_CREATE_LOCK = "blog:article:create:lock:";
    public static final String VIEW_SYNC_LOCK = "blog:article:view:sync:lock";
    public static final String SUMMARY_DONE_PREFIX = "blog:summary:done:";

    //点赞
    public static final String ARTICLE_LIKE_COUNT_KEY = "blog:article:like:count:";
    public static final String ARTICLE_LIKE_USER_SET_KEY = "blog:article:like:user:";
    public static final String ARTICLE_LIKE_DIRTY_KEY = "blog:article:like:dirty";
    public static final String LIKE_SYNC_LOCK = "blog:article:like:sync:lock";
    public static final String LIKE_DONE_PREFIX = "blog:like:done:";

    //浏览记录
    public static final String USER_HISTORY_KEY = "blog:user:history:";
    public static final String USER_HISTORY_DIRTY_KEY = "blog:user:history:dirty";
    public static final String HISTORY_SYNC_LOCK = "blog:user:history:sync:lock";
    public static final String HISTORY_DONE_PREFIX = "blog:history:done:";
    public static final long USER_HISTORY_MAX = 50L;       // 每个用户最多保留50条
}