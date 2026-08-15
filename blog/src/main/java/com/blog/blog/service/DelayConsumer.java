package com.blog.blog.service;

import com.blog.blog.config.RabbitMQConfig;
import com.blog.blog.dto.DelayMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 延迟消息消费者（监听 TTL 到期后路由到 DELAY_PROCESS_QUEUE 的消息）
 * <p>
 * 延迟消息流：生产者 → DELAY_EXCHANGE → DELAY_QUEUE（30s TTL）
 * → TTL 到期 → DLX_EXCHANGE → DELAY_PROCESS_QUEUE → 本消费者
 */
@Component
public class DelayConsumer {
    private static final Logger log = LoggerFactory.getLogger(DelayConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.DELAY_PROCESS_QUEUE)
    public void handleDelayedMessage(DelayMessage message) {
        log.info("收到延迟消息, bizType={}, bizId={}, userId={}, 延迟时间={}ms",
                message.getBizType(), message.getBizId(), message.getUserId(),
                System.currentTimeMillis() - message.getTimestamp());

        try {
            switch (message.getBizType()) {
                case "order_cancel":
                    handleOrderCancel(message);
                    break;
                case "article_publish":
                    handleArticlePublish(message);
                    break;
                case "remind":
                    handleRemind(message);
                    break;
                default:
                    log.warn("未知延迟消息类型, bizType={}", message.getBizType());
                    break;
                case "test":
                    log.info("测试延迟消息验证通过, bizId={}, 实际延迟={}ms",
                            message.getBizId(),
                            System.currentTimeMillis() - message.getTimestamp());
                    break;
            }
        } catch (Exception e) {
            log.error("延迟消息处理失败, bizType={}, bizId={}",
                    message.getBizType(), message.getBizId(), e);
            throw new RuntimeException("延迟消息处理失败, 进入死信队列", e);
        }
    }

    //具体业务

    private void handleOrderCancel(DelayMessage message) {
        // TODO: 查询订单状态，若仍为未支付则取消
        log.info("模拟订单取消, orderId={}, userId={}", message.getBizId(), message.getUserId());
    }

    private void handleArticlePublish(DelayMessage message) {
        // TODO: 定时发布文章
        log.info("模拟定时发布文章, articleId={}, userId={}", message.getBizId(), message.getUserId());
    }

    private void handleRemind(DelayMessage message) {
        // TODO: 延迟提醒（如：新评论通知延迟 30 秒再推送，避免刷屏）
        log.info("模拟延迟提醒, bizId={}, userId={}, payload={}",
                message.getBizId(), message.getUserId(), message.getPayload());
    }
}