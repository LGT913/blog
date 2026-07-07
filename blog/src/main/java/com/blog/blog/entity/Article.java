package com.blog.blog.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="article")
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String title;

    @Lob
    private String content;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String categoryId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column(nullable=false)
    private LocalDateTime updateTime;

    //getter和setter
    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getTitle() {return title;}
    public void setTitle(String title) {this.title = title;}
    public String getContent() {return content;}
    public void setContent(String content) {this.content = content;}
    public Long getUserId() {return userId;}
    public void setUserId(Long userId) {this.userId = userId;}
    public String getCategoryId() {return categoryId;}
    public void setCategoryId(String categoryId) {this.categoryId = categoryId;}
    public LocalDateTime getCreateTime() {return createTime;}
    public void setCreateTime(LocalDateTime createTime) {this.createTime = createTime;}
    public LocalDateTime getUpdateTime() {return updateTime;}
    public void setUpdateTime(LocalDateTime updateTime) {this.updateTime = updateTime;}
}
