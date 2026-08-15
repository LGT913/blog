package com.blog.blog.dto;

import lombok.Data;
import java.io.Serializable;

//注：有表对应才是 Entity,没表对应只是传数据的就是 DTO。
//DTO 后端自己用,VO 给前端用。
@Data
public class CommentNotifyMessage implements Serializable {
    private Long articleId;        // 文章 ID
    private Long authorId;         // 文章作者 ID(通知接收方)
    private Long commentUserId;    // 评论者 ID
    private String commentContent; // 评论内容
    private Long timestamp;        // 发送时间戳(用于幂等去重)
}