package com.blog.blog.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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

    //getter和setter
    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public Long getArticleId() {return articleId;}
    public void setArticleId(Long articleId) {this.articleId = articleId;}
    public Long getUserId() {return userId;}
    public void setUserId(Long userId) {this.userId = userId;}
    public String getContent() {return content;}
    public void setContent(String content) {this.content = content;}
    public Long getParentId() {return parentId;}
    public void setParentId(Long parentId) {this.parentId = parentId;}
    public LocalDateTime getCreateTime() {return createTime;}
    public void setCreateTime(LocalDateTime createTime) {this.createTime = createTime;}
}
