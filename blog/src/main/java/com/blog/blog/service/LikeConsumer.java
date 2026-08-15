package com.blog.blog.service;

import com.blog.blog.common.constant.BlogCacheConstants;
import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.config.RabbitMQConfig;
import com.blog.blog.dto.LikeMessage;
import com.blog.blog.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LikeConsumer {
    private static final Logger log = LoggerFactory.getLogger(LikeConsumer.class);

    private final ArticleRepository articleRepository;
    private final RedisUtil redisUtil;

    @RabbitListener(queues = RabbitMQConfig.LIKE_QUEUE)
    @Transactional
    public void handleLikeMessage(LikeMessage message) {
        // 幂等性检查
        String doneKey = BlogCacheConstants.LIKE_DONE_PREFIX
                + message.getArticleId() + ":" + message.getUserId()
                + ":" + message.getTimestamp();
        Boolean isFirst = redisUtil.setIfAbsent(doneKey, "1", 86400);
        if (!Boolean.TRUE.equals(isFirst)) {
            log.info("点赞消息已处理,跳过, articleId={}, userId={}",
                    message.getArticleId(), message.getUserId());
            return;
        }

        try {
            articleRepository.incrementLikeCount(message.getArticleId(), message.getDelta());
            log.info("点赞持久化成功, articleId={}, delta={}", message.getArticleId(), message.getDelta());
        } catch (Exception e) {
            log.error("点赞持久化失败, articleId={}", message.getArticleId(), e);
            throw new RuntimeException("点赞持久化失败,进入死信队列", e);
        }
    }
}