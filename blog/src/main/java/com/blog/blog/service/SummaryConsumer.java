package com.blog.blog.service;

import com.blog.blog.common.constant.BlogCacheConstants;
import com.blog.blog.common.util.CacheCleaner;
import com.blog.blog.common.util.RedisRateLimiter;
import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.config.RabbitMQConfig;
import com.blog.blog.dto.SummaryGenerateMessage;
import com.blog.blog.entity.Article;
import com.blog.blog.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SummaryConsumer {
    private static final Logger log = LoggerFactory.getLogger(SummaryConsumer.class);

    private final ArticleRepository articleRepository;
    private final DeepSeekService deepSeekService;
    private final RedisRateLimiter rateLimiter;
    private final RedisUtil redisUtil;
    private final CacheCleaner cacheCleaner;

    @RabbitListener(queues = RabbitMQConfig.SUMMARY_QUEUE, concurrency = "2-5")
    public void handleSummaryGeneration(SummaryGenerateMessage message) {
        Long articleId = message.getArticleId();
        log.info("开始异步生成文章摘要, articleId={}", articleId);

        // ① 幂等性检查
        String doneKey = BlogCacheConstants.SUMMARY_DONE_PREFIX + articleId;
        Boolean isFirst = redisUtil.setIfAbsent(doneKey, "1", 86400);
        if (!Boolean.TRUE.equals(isFirst)) {
            log.info("摘要已生成, 跳过重复消费, articleId={}", articleId);
            return;
        }

        // ② 限流检查
        boolean allowed = rateLimiter.tryAcquire("deepseek:summary", 5, 3);
        if (!allowed) {
            log.warn("AI API 限流, articleId={}, 稍后重试", articleId);
            redisUtil.delete(doneKey);
            throw new RuntimeException("AI API 限流, 进入死信队列");
        }

        // ③ 业务逻辑
        Article article;
        try {
            article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new IllegalArgumentException("文章不存在:" + articleId));
        } catch (IllegalArgumentException e) {
            log.error("摘要生成失败, 文章不存在, articleId={}", articleId, e);
            return;
        }

        String summary;
        try {
            summary = deepSeekService.generateSummary(article.getContent());
        } catch (Exception e) {
            log.error("AI 摘要生成失败(可重试), articleId={}", articleId, e);
            throw new RuntimeException("摘要生成失败, 进入死信队列", e);
        }

        updateArticleSummary(articleId, summary);
        cacheCleaner.clearArticleRelatedCaches(articleId);

        log.info("文章摘要生成完成, articleId={}", articleId);
    }

    @Transactional
    public void updateArticleSummary(Long articleId, String summary) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在:" + articleId));
        article.setSummary(summary);
        articleRepository.save(article);
    }
}