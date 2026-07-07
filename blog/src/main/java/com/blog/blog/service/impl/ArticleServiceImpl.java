package com.blog.blog.service.impl;

import com.blog.blog.common.RedisUtil;
import com.blog.blog.entity.Article;
import com.blog.blog.repository.ArticleRepository;
import com.blog.blog.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private RedisUtil redisUtil;

    // Redis key 常量
    private static final String ARTICLE_LIST_KEY = "article:list";
    private static final String ARTICLE_DETAIL_KEY = "article:detail:";

    @Override
    public Article createArticle(String title, String content, Long userId, String categoryId) {
        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setUserId(userId);
        article.setCategoryId(categoryId);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        article = articleRepository.save(article);

        // 创建文章后，删除列表缓存（下次查询时重新加载）
        redisUtil.delete(ARTICLE_LIST_KEY);

        return article;
    }

    @Override
    public Article getArticle(Long id) {
        String key = ARTICLE_DETAIL_KEY + id;

        // 1. 先从 Redis 查
        Article article = (Article) redisUtil.get(key);
        if (article != null) {
            return article;  // 缓存命中，直接返回
        }

        // 2. 缓存未命中，从数据库查
        article = articleRepository.findById(id).orElseThrow(() -> new RuntimeException("文章不存在"));

        // 3. 写入 Redis，过期时间 30 分钟
        redisUtil.set(key, article, 1800);
        return article;
    }

    @Override
    public List<Article> getAllArticles() {
        // 1. 先从 Redis 查
        List<Article> articles = (List<Article>) redisUtil.get(ARTICLE_LIST_KEY);
        if (articles != null) {
            return articles;  // 缓存命中，直接返回
        }

        // 2. 缓存未命中，从数据库查
        articles = articleRepository.findAllByOrderByCreateTimeDesc();

        // 3. 写入 Redis，过期时间 10 分钟
        redisUtil.set(ARTICLE_LIST_KEY, articles, 600);
        return articles;
    }

    @Override
    public List<Article> getArticlesByUserId(Long userId) {
        // 按用户查询不常用，直接查数据库，不加缓存
        return articleRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    @Override
    public Article updateArticle(Long id, String title, String content) {
        Article article = articleRepository.findById(id).orElseThrow(() -> new RuntimeException("文章不存在"));
        article.setTitle(title);
        article.setContent(content);
        article.setUpdateTime(LocalDateTime.now());
        article = articleRepository.save(article);

        // 更新后，删除列表缓存和详情缓存
        redisUtil.delete(ARTICLE_LIST_KEY);
        redisUtil.set(ARTICLE_DETAIL_KEY + id, article, 1800);

        return article;
    }

    @Override
    public void deleteArticle(Long id) {
        Article article = articleRepository.findById(id).orElseThrow(() -> new RuntimeException("文章不存在"));
        articleRepository.delete(article);

        // 删除后，清除列表缓存和详情缓存
        redisUtil.delete(ARTICLE_LIST_KEY);
        redisUtil.delete(ARTICLE_DETAIL_KEY + id);
    }
}
