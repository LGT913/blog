package com.blog.blog.service;

import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.config.RabbitMQConfig;
import com.blog.blog.dto.CommentNotifyMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentNotifyConsumer {
    private static final Logger log = LoggerFactory.getLogger(CommentNotifyConsumer.class);

    private static final String COMMENT_NOTIFY_DONE_PREFIX = "blog:comment:notify:done:";

    /** 文章详情页评论推送目标: /topic/article/{articleId}/comments */
    private static final String COMMENT_TOPIC_TEMPLATE = "/topic/article/%d/comments";

    private final RedisUtil redisUtil;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.COMMENT_QUEUE)
    public void handleCommentNotify(CommentNotifyMessage message) {
        // 幂等性检查:基于文章ID+评论者ID+时间戳去重
        String idempotentKey = COMMENT_NOTIFY_DONE_PREFIX
                + message.getArticleId() + ":" + message.getCommentUserId()
                + ":" + message.getTimestamp();
        Boolean isFirst = redisUtil.setIfAbsent(idempotentKey, "1", 86400);
        if (!Boolean.TRUE.equals(isFirst)) {
            log.info("评论通知已处理,跳过重复消费, articleId={}, commentUserId={}",
                    message.getArticleId(), message.getCommentUserId());
            return;
        }

        try {
            log.info("收到评论通知:文章ID={}, 作者ID={}, 评论者ID={}, 内容={}",
                    message.getArticleId(),
                    message.getAuthorId(),
                    message.getCommentUserId(),
                    message.getCommentContent());

            // WebSocket 实时推送到文章详情页的所有用户
            String destination = String.format(COMMENT_TOPIC_TEMPLATE, message.getArticleId());
            messagingTemplate.convertAndSend(destination, message);
            log.info("WebSocket 推送成功, destination={}", destination);
        } catch (Exception e) {
            log.error("处理评论通知失败,message={}", message, e);
            throw new RuntimeException(e);  // 抛出后触发 NACK → 死信队列
        }
    }
}