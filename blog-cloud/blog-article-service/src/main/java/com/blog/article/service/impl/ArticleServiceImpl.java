package com.blog.article.service.impl;

import com.blog.article.config.ArticleConfig;
import com.blog.article.entity.Article;
import com.blog.article.repository.ArticleRepository;
import com.blog.article.service.ArticleService;
import com.blog.common.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleConfig articleConfig;

    private final ArticleRepository articleRepository;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Article createArticle(String title, String content, Long userId, String categoryId) {
        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setUserId(userId);
        article.setCategoryId(categoryId);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        return articleRepository.save(article);
    }

    @Override
    public Article getArticle(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
    }

    @Override
    public List<Article> getAllArticles() {
        return articleRepository.findAllByOrderByCreateTimeDesc();
    }

    @Override
    public List<Article> getArticlesByUserId(Long userId) {
        return articleRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    @Override
    @Transactional
    public Article updateArticle(Long id, String title, String content, String categoryId) {
        Article article = getArticle(id);
        if (title != null) article.setTitle(title);
        if (content != null) article.setContent(content);
        if (categoryId != null) article.setCategoryId(categoryId);
        article.setUpdateTime(LocalDateTime.now());
        return articleRepository.save(article);
    }

    @Override
    @Transactional
    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
    }

    @Override
    public List<Article> getArticleRankingByViews() {
        return articleRepository.findTop10ByOrderByViewCountDesc();
    }

    @Override
    public List<Article> getArticleRankingByLatest() {
        return articleRepository.findTop10ByOrderByCreateTimeDesc();
    }

    @Override
    public PageResult<Article> getAllArticlesPage(int page, int size, String categoryId, String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> articlePage;

        if (categoryId != null && !categoryId.isEmpty() && keyword != null && !keyword.isEmpty()) {
            articlePage = articleRepository.searchByCategoryAndKeyword(categoryId, "%" + keyword + "%", pageable);
        } else if (categoryId != null && !categoryId.isEmpty()) {
            articlePage = articleRepository.findByCategoryIdOrderByCreateTimeDesc(categoryId, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            articlePage = articleRepository.findByTitleContainingOrContentContainingOrderByCreateTimeDesc(keyword, keyword, pageable);
        } else {
            articlePage = articleRepository.findAllByOrderByCreateTimeDesc(pageable);
        }

        return PageResult.of(articlePage);
    }

    // 以下方法第 3 阶段补全（Redis/RabbitMQ 相关）
    @Override
    public void incrementViewCount(Long articleId) {
        //Redis 阅读计数
        String key = articleConfig.getPrefix() + ":article:view_count";
        stringRedisTemplate.opsForZSet().incrementScore(key, articleId.toString(), 1);
    }

    @Override
    @Scheduled(fixedDelay = 300000) // 每5分钟
    @Transactional
    public void syncViewCountToDB() {
        // 定时同步
        String key = articleConfig.getPrefix() + ":article:view_count";
        Set<ZSetOperations.TypedTuple<String>> topArticles =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);
        if (topArticles == null || topArticles.isEmpty()) {
            return;
        }
        for (ZSetOperations.TypedTuple<String> tuple : topArticles) {
            Long articleId = Long.valueOf(tuple.getValue());
            int delta = Optional.ofNullable(tuple.getScore()).map(Double::intValue).orElse(0);
            if (delta > 0) {
                articleRepository.incrementViewCount(articleId, delta);
                stringRedisTemplate.opsForZSet().incrementScore(key, articleId.toString(), -delta);
            }
        }
    }
}