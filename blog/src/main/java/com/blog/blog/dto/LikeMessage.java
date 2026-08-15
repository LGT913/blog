package com.blog.blog.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class LikeMessage implements Serializable {
    private Long articleId;
    private Long userId;
    private int delta;          // 1=点赞, -1=取消点赞
    private Long timestamp;
}