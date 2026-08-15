package com.blog.blog.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    //业务交换机/队列/路由键
    public static final String COMMENT_EXCHANGE = "blog.comment.exchange";
    public static final String COMMENT_QUEUE = "blog.comment.queue";
    public static final String COMMENT_ROUTING_KEY = "blog.comment.notify";

    public static final String SUMMARY_EXCHANGE = "blog.summary.exchange";
    public static final String SUMMARY_QUEUE = "blog.summary.queue";
    public static final String SUMMARY_ROUTING_KEY = "blog.summary.generate";

    //点赞
    public static final String LIKE_EXCHANGE = "blog.like.exchange";
    public static final String LIKE_QUEUE = "blog.like.queue";
    public static final String LIKE_ROUTING_KEY = "blog.like.save";

    //浏览记录
    public static final String HISTORY_EXCHANGE = "blog.history.exchange";
    public static final String HISTORY_QUEUE = "blog.history.queue";
    public static final String HISTORY_ROUTING_KEY = "blog.history.save";

    //统一死信队列（处理消费失败的消息）
    public static final String DLX_EXCHANGE = "blog.dlx.exchange";
    public static final String DLX_QUEUE = "blog.dlx.queue";
    public static final String DLX_ROUTING_KEY = "blog.dlx";

    //延迟队列（TTL + DLX 模式，模拟订单定时任务）
    public static final String DELAY_EXCHANGE = "blog.delay.exchange";
    public static final String DELAY_QUEUE = "blog.delay.queue";
    public static final String DELAY_ROUTING_KEY = "blog.delay";
    public static final String DELAY_PROCESS_QUEUE = "blog.delay.process.queue";
    public static final long DELAY_TTL_MS = 30_000; // 30 秒延迟，按需调整

    //业务交换机
    @Bean
    public TopicExchange commentExchange() {
        return new TopicExchange(COMMENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange summaryExchange() {
        return new TopicExchange(SUMMARY_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange likeExchange() {
        return new TopicExchange(LIKE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange historyExchange() {
        return new TopicExchange(HISTORY_EXCHANGE, true, false);
    }

    //业务队列（统一使用 DLX_EXCHANGE 作为死信）
    @Bean
    public Queue commentQueue() {
        return QueueBuilder.durable(COMMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue summaryQueue() {
        // 注意：业务队列不加 x-message-ttl，防止消费积压时消息被意外过期
        return QueueBuilder.durable(SUMMARY_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue likeQueue() {
        return QueueBuilder.durable(LIKE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue historyQueue() {
        return QueueBuilder.durable(HISTORY_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    //业务绑定
    @Bean
    public Binding commentBinding() {
        return BindingBuilder.bind(commentQueue())
                .to(commentExchange())
                .with(COMMENT_ROUTING_KEY);
    }

    @Bean
    public Binding summaryBinding() {
        return BindingBuilder.bind(summaryQueue())
                .to(summaryExchange())
                .with(SUMMARY_ROUTING_KEY);
    }

    @Bean
    public Binding likeBinding() {
        return BindingBuilder.bind(likeQueue())
                .to(likeExchange())
                .with(LIKE_ROUTING_KEY);
    }

    @Bean
    public Binding historyBinding() {
        return BindingBuilder.bind(historyQueue())
                .to(historyExchange())
                .with(HISTORY_ROUTING_KEY);
    }

    //统一死信交换机/队列
    // 所有业务消费失败的消息 → DLX_EXCHANGE → DLX_QUEUE（可在管理页面查看）
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with(DLX_ROUTING_KEY);
    }

    /* 延迟队列（TTL + DLX 模式）
     生产者 → DELAY_EXCHANGE → DELAY_QUEUE（TTL 到期）
                                               ↓
                                         DLX_EXCHANGE
                                               ↓
                                   DELAY_PROCESS_QUEUE（消费者监听）

     与 DLX_QUEUE 通过不同的 routing key 隔离，失败消息和延迟消息互不干扰 */
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE, true, false);
    }

    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .withArgument("x-message-ttl", DELAY_TTL_MS)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DELAY_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue())
                .to(delayExchange())
                .with(DELAY_ROUTING_KEY);
    }

    // 延迟消息的处理队列（绑定到 DLX_EXCHANGE，使用 DELAY_ROUTING_KEY 路由）
    @Bean
    public Queue delayProcessQueue() {
        return QueueBuilder.durable(DELAY_PROCESS_QUEUE).build();
    }

    @Bean
    public Binding delayProcessBinding() {
        return BindingBuilder.bind(delayProcessQueue())
                .to(dlxExchange())
                .with(DELAY_ROUTING_KEY);
    }

    //通用配置
    // Jackson 序列化（跨语言、安全、可读）
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}