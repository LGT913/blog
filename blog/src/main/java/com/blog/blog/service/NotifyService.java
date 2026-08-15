package com.blog.blog.service;

import com.blog.blog.config.RabbitMQConfig;
import com.blog.blog.dto.CommentNotifyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotifyService {
    private static final Logger log = LoggerFactory.getLogger(NotifyService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendCommentNotify(CommentNotifyMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.COMMENT_EXCHANGE,
                    RabbitMQConfig.COMMENT_ROUTING_KEY,
                    message
            );
        } catch (Exception e) {
            // try-catch 降级:RabbitMQ 不可用时不影响主流程,只记日志
            log.error("发送评论通知失败,articleId={}", message.getArticleId(), e);
        }
    }
}