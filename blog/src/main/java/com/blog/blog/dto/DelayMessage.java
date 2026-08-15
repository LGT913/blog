package com.blog.blog.dto;

import lombok.Data;
import java.io.Serializable;

//延迟消息 DTO（模拟订单定时任务、延迟通知等场景）
@Data
public class DelayMessage implements Serializable {
    private String bizType;       // 业务类型：order_cancel / article_publish / remind
    private String bizId;         // 业务 ID（如 orderId、articleId）
    private Long userId;          // 关联用户
    private String payload;       // 扩展数据（JSON），按需携带
    private Long timestamp;       // 生产时间戳
}