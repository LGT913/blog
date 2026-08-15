package com.blog.blog.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long articleId;

    @Column(nullable=false)
    private Long userId;

    @Column(nullable=false)
    private String content;

    @Column(name="parent_id",nullable=true)
    private Long parentId;

    @Column(nullable=false)
    private LocalDateTime createTime;

    /** 评论者昵称（非数据库字段，查询时批量填充，用于前端展示） */
    @Transient
    private String username;
}
