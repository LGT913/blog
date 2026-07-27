package com.blog.blog.service.impl;

import com.blog.blog.common.BloomFilterUtil;
import com.blog.blog.common.RedisUtil;
import com.blog.blog.entity.Article;
import com.blog.blog.repository.ArticleRepository;
import com.blog.blog.service.ArticleService;
import com.blog.blog.service.DeepSeekService;
import com.blog.blog.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private BloomFilterUtil bloomFilterUtil;

    // Redis key 常量
    private static final String ARTICLE_LIST_KEY = "article:list"; //所有文章的列表数据
    private static final String ARTICLE_LIST_PAGE_KEY = "article:list:page:"; //分页文章数据的前缀
    private static final String ARTICLE_TOTAL_COUNT_KEY = "article:total:count";  //全局文章总数缓存 key
    private static final String ARTICLE_DETAIL_KEY_PREFIX = "article:detail:";  //单篇文章的详情数据
    private static final String ARTICLE_RANKING_VIEWS_KEY = "article:ranking:views";  //按阅读量排行的前10篇文章
    private static final String ARTICLE_RANKING_LATEST_KEY = "article:ranking:latest";  //按创建时间排行的前10篇文章
    private static final long ARTICLE_LIST_EXPIRE = 600L;
    private static final long ARTICLE_DETAIL_EXPIRE = 1800L;
    private static final long ARTICLE_RANKING_EXPIRE = 300L;
    private static final long ARTICLE_TOTAL_EXPIRE = 600L;  //总数缓存过期时间，和列表保持一致
    private static final long NULL_CACHE_EXPIRE = 60L;  //空值缓存过期时间（秒）
    // 统一使用静态 RANDOM（问题7）
    private static final java.util.Random RANDOM = new java.util.Random();

    //删除缓存（Bug2+Bug3：改用模糊匹配删除所有分页缓存，包括分类缓存）
    private void clearRelatedCaches(Long articleId) {
        redisUtil.delete(ARTICLE_LIST_KEY);
        // 模糊删除所有分页缓存：article:list:page:* 匹配 page:0、page:0:cat:1 等所有变体
        redisUtil.deleteByPattern(ARTICLE_LIST_PAGE_KEY + "*");
        // 模糊删除所有分类总数缓存：article:total:count* 匹配全局和 :cat:x
        redisUtil.deleteByPattern(ARTICLE_TOTAL_COUNT_KEY + "*");
        redisUtil.delete(ARTICLE_DETAIL_KEY_PREFIX + articleId);
        redisUtil.delete(ARTICLE_RANKING_VIEWS_KEY);
        redisUtil.delete(ARTICLE_RANKING_LATEST_KEY);
    }

    @Override
    public Article createArticle(String title, String content, Long userId, String categoryId) {
        //创建Article对象并赋值
        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setUserId(userId);
        article.setCategoryId(categoryId);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());

        //调用AI生成摘要
        String summary=deepSeekService.generateSummary(content);
        article.setSummary(summary);

        article = articleRepository.save(article);
        // 新增文章后，把 ID 加入布隆过滤器
        bloomFilterUtil.add(article.getId());

        redisUtil.delete(ARTICLE_LIST_KEY);

        // 创建文章后，删除列表缓存（下次查询时重新加载）
        clearRelatedCaches(article.getId());

        return article;
    }

    @Override
    public PageResult<Article> getAllArticlesPage(int page, int size, String categoryId, String keyword) {
        // 【参数校验】防止前端乱传导致 400
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        boolean hasCategory = categoryId != null && !categoryId.isEmpty();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String kw = hasKeyword ? keyword.trim() : null;

        // 缓存 key 按分类+关键词组合区分
        String listKey = ARTICLE_LIST_PAGE_KEY + page
                + (hasCategory ? ":cat:" + categoryId : "")
                + (hasKeyword ? ":kw:" + kw : "");
        String totalKey = ARTICLE_TOTAL_COUNT_KEY
                + (hasCategory ? ":cat:" + categoryId : "")
                + (hasKeyword ? ":kw:" + kw : "");

        // 1. 先从 Redis 查缓存
        Object cacheObj = redisUtil.get(listKey);
        Object totalObj = redisUtil.get(totalKey);

        // 2. 缓存命中
        if (cacheObj instanceof List && totalObj instanceof Number) {
            @SuppressWarnings("unchecked")
            List<Article> cachedList = (List<Article>) cacheObj;
            long total = ((Number) totalObj).longValue();
            return PageResult.of(cachedList, total, page, size);
        }

        // 3. 缓存未命中，从数据库查询
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> pageResult;

        if (hasKeyword && hasCategory) {
            pageResult = articleRepository
                    .searchByCategoryAndKeyword(
                            categoryId, kw, pageable);
        } else if (hasKeyword) {
            pageResult = articleRepository
                    .findByTitleContainingOrContentContainingOrderByCreateTimeDesc(kw, kw, pageable);
        } else if (hasCategory) {
            pageResult = articleRepository.findByCategoryIdOrderByCreateTimeDesc(categoryId, pageable);
        } else {
            pageResult = articleRepository.findAllByOrderByCreateTimeDesc(pageable);
        }

        // 4. 写入 Redis 缓存（加随机过期时间防雪崩，统一使用静态 RANDOM）
        long expire = ARTICLE_LIST_EXPIRE + RANDOM.nextInt(120);

        if (pageResult.hasContent()) {
            redisUtil.set(listKey, pageResult.getContent(), expire);
            redisUtil.set(totalKey, pageResult.getTotalElements(), ARTICLE_TOTAL_EXPIRE + RANDOM.nextInt(120));
        } else {
            // 缓存空值防穿透（短过期）
            redisUtil.set(listKey, Collections.emptyList(), NULL_CACHE_EXPIRE);
            redisUtil.set(totalKey, 0L, NULL_CACHE_EXPIRE);
        }

        return PageResult.of(pageResult);
    }



    @Override
    public Article getArticle(Long id) {
        // 第 0 关：布隆过滤器 —— 不存在的 ID 直接拦截，不查缓存不查库
        if (!bloomFilterUtil.mightContain(id)) {
            throw new RuntimeException("文章不存在");
        }

        String key = ARTICLE_DETAIL_KEY_PREFIX + id;

        // 1. 先从 Redis 查
        Object cacheObj = redisUtil.get(key);
        if (cacheObj != null) {
            // 空值标记防穿透：缓存了 "NULL" 说明该 id 不存在
            if ("NULL".equals(cacheObj)) {
                throw new RuntimeException("文章不存在");
            }
            return (Article) cacheObj;
        }

        // 2. 缓存未命中，加分布式锁防止缓存击穿（Bug1：改用 redisUtil.setIfAbsent）
        String lockKey = "article:lock:" + id;
        String lockValue = UUID.randomUUID().toString();  // 唯一标识，用于安全释放锁（问题4a）
        int retryCount = 0;

        while (retryCount < 3) {
                Boolean locked = redisUtil.setIfAbsent(lockKey, lockValue, 10);

            if (Boolean.TRUE.equals(locked)) {
                try {
                    // 3. 二次检查
                    cacheObj = redisUtil.get(key);
                    if (cacheObj != null) {
                        if ("NULL".equals(cacheObj)) {
                            throw new RuntimeException("文章不存在");
                        }
                        return (Article) cacheObj;
                    }

                    // 4. 从数据库查
                    java.util.Optional<Article> articleOpt = articleRepository.findById(id);

                    if (articleOpt.isEmpty()) {
                        // 问题1：缓存空值防穿透，短过期
                        redisUtil.set(key, "NULL", NULL_CACHE_EXPIRE);
                        throw new RuntimeException("文章不存在");
                    }

                    // 5. 写入缓存（加随机过期时间防雪崩，统一使用静态 RANDOM）
                    long expire = ARTICLE_DETAIL_EXPIRE + RANDOM.nextInt(300);
                    redisUtil.set(key, articleOpt.get(), expire);
                    return articleOpt.get();
                } finally {
                    // 问题4a：用 Lua 脚本安全释放锁，只有持有者才能删
                    redisUtil.unlock(lockKey, lockValue);
                }
            }

            // 6. 没拿到锁，等待后重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            retryCount++;
        }

        // 7. 重试 3 次还没拿到锁，直接查数据库
        return articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
    }


    @Override
    public List<Article> getAllArticles() {
        // 1. 先从 Redis 查
        Object cacheObj = redisUtil.get(ARTICLE_LIST_KEY);
        if (cacheObj != null) {
            if (cacheObj instanceof List) {
                return (List<Article>) cacheObj;
            }
        }

        // 2. 缓存未命中，从数据库查
        List<Article> articles = articleRepository.findAllByOrderByCreateTimeDesc();

        // 3. 写入 Redis（问题6：加随机过期时间防雪崩）
        if (articles.isEmpty()) {
            // 问题2：缓存空值防穿透
            redisUtil.set(ARTICLE_LIST_KEY, Collections.emptyList(), NULL_CACHE_EXPIRE);
        } else {
            redisUtil.set(ARTICLE_LIST_KEY, articles, ARTICLE_LIST_EXPIRE + RANDOM.nextInt(120));
        }
        return articles;
    }

    @Override
    public List<Article> getArticlesByUserId(Long userId) {
        // 按用户查询不常用，直接查数据库，不加缓存
        return articleRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    @Override
    public Article updateArticle(Long id, String title, String content, String categoryId) {
        Article article = articleRepository.findById(id).orElseThrow(() -> new RuntimeException("文章不存在"));
        article.setTitle(title);
        article.setContent(content);
        if (categoryId != null && !categoryId.isEmpty()) {
            article.setCategoryId(categoryId);
        }
        article.setUpdateTime(LocalDateTime.now());
        article = articleRepository.save(article);

        // 更新数据 → 只删除缓存，不主动写入缓存
        clearRelatedCaches(article.getId());

        return article;
    }

    @Override
    public void deleteArticle(Long id) {
        Article article = articleRepository.findById(id).orElseThrow(() -> new RuntimeException("文章不存在"));
        articleRepository.delete(article);

        // 删除数据 → 清理对应所有缓存
        clearRelatedCaches(article.getId());
    }

    @Override
    public List<Article> getArticleRankingByViews() {
        // 1. 先从 Redis 查
        Object cacheObj = redisUtil.get(ARTICLE_RANKING_VIEWS_KEY);
        if (cacheObj != null) {
            if (cacheObj instanceof List) {
                return (List<Article>) cacheObj;
            }
        }

        // 2. 缓存未命中，从数据库查（取前10条，按阅读量降序）
        List<Article> articles = articleRepository.findTop10ByOrderByViewCountDesc();

        // 3. 写入 Redis（问题6：加随机过期时间防雪崩）
        if (articles.isEmpty()) {
            // 问题2：缓存空值防穿透
            redisUtil.set(ARTICLE_RANKING_VIEWS_KEY, Collections.emptyList(), NULL_CACHE_EXPIRE);
        } else {
            redisUtil.set(ARTICLE_RANKING_VIEWS_KEY, articles, ARTICLE_RANKING_EXPIRE + RANDOM.nextInt(60));
        }
        return articles;
    }

    @Override
    public List<Article> getArticleRankingByLatest() {
        // 1. 先从 Redis 查
        Object cacheObj = redisUtil.get(ARTICLE_RANKING_LATEST_KEY);
        if (cacheObj != null) {
            if (cacheObj instanceof List) {
                return (List<Article>) cacheObj;
            }
        }

        // 2. 缓存未命中，从数据库查（取前10条，按创建时间降序）
        List<Article> articles = articleRepository.findTop10ByOrderByCreateTimeDesc();

        // 3. 写入 Redis（问题6：加随机过期时间防雪崩）
        if (articles.isEmpty()) {
            // 问题2：缓存空值防穿透
            redisUtil.set(ARTICLE_RANKING_LATEST_KEY, Collections.emptyList(), NULL_CACHE_EXPIRE);
        } else {
            redisUtil.set(ARTICLE_RANKING_LATEST_KEY, articles, ARTICLE_RANKING_EXPIRE + RANDOM.nextInt(60));
        }
        return articles;
    }
}
