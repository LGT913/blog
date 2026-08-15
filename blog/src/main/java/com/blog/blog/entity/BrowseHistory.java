package com.blog.blog.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "browse_history", indexes = {
        @Index(name = "idx_user_time", columnList = "userId, browse_time DESC")})
public class BrowseHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long articleId;

    @Column(nullable = false)
    private LocalDateTime browseTime;
}