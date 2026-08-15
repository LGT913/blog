package com.blog.blog.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false,unique = true)
    private String username;

    @Column(nullable=false)
    private String password;

    @Column(nullable=false)
    private String nickname;

    @Column(nullable=false)
    private LocalDateTime createTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private Boolean enabled = true;  // 账号状态：DB 是数据真相源，Redis 禁用标记只作为加速缓存

    public enum Role {
        USER,
        ADMIN
    }
}
