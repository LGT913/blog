package com.blog.blog.config;

import com.blog.blog.common.constant.BlogCacheConstants;
import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.entity.Article;
import com.blog.blog.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 缓存预热启动器
 * 应用启动时自动加载热点数据到 Redis,避免冷启动时所有请求打 DB
 *
 * 优先级:布隆过滤器(最先,在 BloomFilterUtil.init() 同步执行)
 *       > ZSet排行榜(同步,保证首页立即可用)
 *       > 列表缓存(异步) > 详情缓存(异步)
 *
 * 关键注意:@Async 必须通过 Spring 代理调用才生效
 *        本类通过 applicationContext.getBean() 获取代理对象后再调用异步方法
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class CacheWarmup implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CacheWarmup.class);

    private final RedisUtil redisUtil;
    private final ArticleRepository articleRepository;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 开始缓存预热 ==========");
        long start = System.currentTimeMillis();

        try {
            // 1. 同步预热 ZSet 排行榜(立即可用,保证首页不查 DB)
            warmupZSetRanking();

            // 2. 通过 Spring 代理调用异步方法,确保 @Async 生效(修复同类内部调用不生效的 Bug)
            CacheWarmup proxy = applicationContext.getBean(CacheWarmup.class);
            proxy.warmupListCachesAsync();
            proxy.warmupDetailCachesAsync();

            log.info("========== 缓存预热启动完成(同步部分),耗时:{}ms ===========",
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 预热失败不影响服务启动,只记告警
            log.warn("缓存预热失败,不影响服务启动", e);
        }
    }

    /**
     * 预热 ZSet 阅读排行榜(同步,立即可用)
     * 用户访问 /ranking/views 时直接从 ZSet 取,不用查 DB
     */
    private void warmupZSetRanking() {
        long start = System.currentTimeMillis();
        try {
            // 如果 ZSet 已有数据,说明之前已预热过,不强制覆盖
            if (Boolean.TRUE.equals(redisUtil.hasKey(BlogCacheConstants.ARTICLE_RANKING_ZSET_VIEWS))) {
                Long size = redisUtil.zSize(BlogCacheConstants.ARTICLE_RANKING_ZSET_VIEWS);
                if (size != null && size > 0) {
                    log.info("[预热]ZSet排行榜已存在(size={}),跳过初始化", size);
                    return;
                }
            }

            List<Article> topByViews = articleRepository.findTop10ByOrderByViewCountDesc();
            for (Article a : topByViews) {
                redisUtil.zAdd(BlogCacheConstants.ARTICLE_RANKING_ZSET_VIEWS, a.getId(), a.getViewCount());
            }
            log.info("[预热]ZSet排行榜初始化完成,文章数:{},耗时:{}ms",
                    topByViews.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("[预热]ZSet排行榜失败", e);
        }
    }

    /**
     * 异步预热文章全量列表缓存
     * 注意:这个方法必须通过 Spring 代理调用才会走异步线程池
     */
    @org.springframework.scheduling.annotation.Async("warmupExecutor")
    public void warmupListCachesAsync() {
        long start = System.currentTimeMillis();
        List<Article> all = null;
        try {
            all = articleRepository.findAllByOrderByCreateTimeDesc();
            if (!all.isEmpty()) {
                long expire = BlogCacheConstants.ARTICLE_LIST_EXPIRE + ThreadLocalRandom.current().nextInt(120);
                redisUtil.set(BlogCacheConstants.ARTICLE_LIST_KEY, all, expire);
            } else {
                redisUtil.set(BlogCacheConstants.ARTICLE_LIST_KEY, java.util.Collections.emptyList(),
                        BlogCacheConstants.NULL_CACHE_EXPIRE);
            }
            log.info("[预热-异步]文章列表缓存完成,数量:{},耗时:{}ms",
                    all == null ? 0 : all.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("[预热-异步]列表缓存失败", e);
        }
    }

    /**
     * 异步预热 TOP 10 文章详情缓存(热点文章)
     * 不预热全部文章,避免启动太久,且冷文章访问时自然缓存
     */
    @org.springframework.scheduling.annotation.Async("warmupExecutor")
    public void warmupDetailCachesAsync() {
        long start = System.currentTimeMillis();
        int prewarmCount = 0;
        try {
            List<Article> topByViews = articleRepository.findTop10ByOrderByViewCountDesc();
            for (Article a : topByViews) {
                long expire = BlogCacheConstants.ARTICLE_DETAIL_EXPIRE + ThreadLocalRandom.current().nextInt(300);
                redisUtil.setWithLogicalExpire(BlogCacheConstants.ARTICLE_DETAIL_KEY + a.getId(), a, expire);
                prewarmCount++;
            }
            log.info("[预热-异步]文章详情缓存完成,数量:{},耗时:{}ms",
                    prewarmCount, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("[预热-异步]详情缓存失败", e);
        }
    }
}