package com.blog.blog.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
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

    @Column(name="summary",length=1000)
    private String summary;

    @Column(columnDefinition = "int default 0")
    private Integer viewCount = 0;
}
