package com.blog.blog.service.impl;

import com.blog.blog.common.RedisUtil;
import com.blog.blog.entity.Article;
import com.blog.blog.repository.ArticleRepository;
import com.blog.blog.service.ArticleService;
import com.blog.blog.service.DeepSeekService;
import com.blog.blog.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private DeepSeekService deepSeekService;

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

    //删除缓存
    private void clearRelatedCaches(Long articleId) {
        redisUtil.delete(ARTICLE_LIST_KEY);
        // 删除所有分页缓存（使用通配符，需要RedisTemplate支持）
        // 由于当前RedisUtil不支持模糊删除，这里删除常用分页缓存
        for (int i = 0; i < 10; i++) {
            redisUtil.delete(ARTICLE_LIST_PAGE_KEY + i);
        }
        redisUtil.delete(ARTICLE_DETAIL_KEY_PREFIX + articleId);
        redisUtil.delete(ARTICLE_RANKING_VIEWS_KEY);
        redisUtil.delete(ARTICLE_RANKING_LATEST_KEY);
        redisUtil.delete(ARTICLE_TOTAL_COUNT_KEY); //清除总数缓存
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

        // 创建文章后，删除列表缓存（下次查询时重新加载）
        clearRelatedCaches(article.getId());

        return article;
    }

    @Override
    public PageResult<Article> getAllArticlesPage(int page, int size) {
        // 【参数校验】防止前端乱传导致 400
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        String listKey = ARTICLE_LIST_PAGE_KEY + page;
        String totalKey = ARTICLE_TOTAL_COUNT_KEY;

        // 1. 先从 Redis 查缓存（同时查列表和总数）
        Object cacheObj = redisUtil.get(listKey);
        Object totalObj = redisUtil.get(totalKey);

        // 2. 缓存命中：列表和总数都存在，直接返回，不查数据库
        if (!ObjectUtils.isEmpty(cacheObj) && !ObjectUtils.isEmpty(totalObj)) {
            @SuppressWarnings("unchecked")
            List<Article> cachedList = (List<Article>) cacheObj;
            long total = ((Number) totalObj).longValue();

            // 使用缓存的列表和总数构造 PageResult，完全不查数据库
            return PageResult.of(cachedList, total, page, size);
        }

        // 3. 缓存未命中：从数据库查询（排序规则封装在 Repository 方法中）
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> pageResult = articleRepository.findAllByOrderByCreateTimeDesc(pageable);

        // 4. 写入 Redis 缓存
        redisUtil.set(listKey, pageResult.getContent(), ARTICLE_LIST_EXPIRE);
        redisUtil.set(totalKey, pageResult.getTotalElements(), ARTICLE_TOTAL_EXPIRE);

        // 5. 返回 PageResult（而不是 PageImpl）
        return PageResult.of(pageResult);
    }


    @Override
    public Article getArticle(Long id) {
        String key = ARTICLE_DETAIL_KEY_PREFIX + id;

        // 1. 先从 Redis 查
        Object cacheObj = redisUtil.get(key);
        if (!ObjectUtils.isEmpty(cacheObj)) {
            return (Article) cacheObj;
        }

        // 2. 缓存未命中，从数据库查
        Article article = articleRepository.findById(id).orElseThrow(() -> new RuntimeException("文章不存在"));

        // 3. 写入 Redis，过期时间 30 分钟
        redisUtil.set(key, article, ARTICLE_DETAIL_EXPIRE);
        return article;
    }

    @Override
    public List<Article> getAllArticles() {
        // 1. 先从 Redis 查
        Object cacheObj = redisUtil.get(ARTICLE_LIST_KEY);
        if (!ObjectUtils.isEmpty(cacheObj)) {
            return (List<Article>) cacheObj;
        }

        // 2. 缓存未命中，从数据库查
        List<Article> articles = articleRepository.findAllByOrderByCreateTimeDesc();

        // 3. 写入 Redis，过期时间 10 分钟
        redisUtil.set(ARTICLE_LIST_KEY, articles, ARTICLE_LIST_EXPIRE);
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
        if (!ObjectUtils.isEmpty(cacheObj)) {
            return (List<Article>) cacheObj;
        }

        // 2. 缓存未命中，从数据库查（取前10条，按阅读量降序）
        List<Article> articles = articleRepository.findTop10ByOrderByViewCountDesc();

        // 3. 写入 Redis，过期时间 5 分钟
        redisUtil.set(ARTICLE_RANKING_VIEWS_KEY, articles, ARTICLE_RANKING_EXPIRE);
        return articles;
    }

    @Override
    public List<Article> getArticleRankingByLatest() {
        // 1. 先从 Redis 查
        Object cacheObj = redisUtil.get(ARTICLE_RANKING_LATEST_KEY);
        if (!ObjectUtils.isEmpty(cacheObj)) {
            return (List<Article>) cacheObj;
        }

        // 2. 缓存未命中，从数据库查（取前10条，按创建时间降序）
        List<Article> articles = articleRepository.findTop10ByOrderByCreateTimeDesc();

        // 3. 写入 Redis，过期时间 5 分钟
        redisUtil.set(ARTICLE_RANKING_LATEST_KEY, articles, ARTICLE_RANKING_EXPIRE);
        return articles;
    }
}
