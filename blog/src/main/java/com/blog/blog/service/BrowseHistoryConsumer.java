package com.blog.blog.service;

import com.blog.blog.common.constant.BlogCacheConstants;
import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.config.RabbitMQConfig;
import com.blog.blog.dto.BrowseHistoryMessage;
import com.blog.blog.entity.BrowseHistory;
import com.blog.blog.repository.BrowseHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BrowseHistoryConsumer {
    private static final Logger log = LoggerFactory.getLogger(BrowseHistoryConsumer.class);

    private final BrowseHistoryRepository browseHistoryRepository;
    private final RedisUtil redisUtil;

    @RabbitListener(queues = RabbitMQConfig.HISTORY_QUEUE)
    @Transactional
    public void handleBrowseHistory(BrowseHistoryMessage message) {
        // 幂等性检查：防止 MQ 重复投递，用 userId:articleId:timestamp 确保同一次浏览只记录一次
        String doneKey = BlogCacheConstants.HISTORY_DONE_PREFIX
                + message.getUserId() + ":" + message.getArticleId()
                + ":" + message.getTimestamp();
        Boolean isFirst = redisUtil.setIfAbsent(doneKey, "1", 86400);
        if (!Boolean.TRUE.equals(isFirst)) {
            return;
        }

        try {
            BrowseHistory history = new BrowseHistory();
            history.setUserId(message.getUserId());
            history.setArticleId(message.getArticleId());
            history.setBrowseTime(LocalDateTime.now());
            browseHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("浏览记录持久化失败, userId={}, articleId={}",
                    message.getUserId(), message.getArticleId(), e);
            throw new RuntimeException("浏览记录持久化失败,进入死信队列", e);
        }
    }
}