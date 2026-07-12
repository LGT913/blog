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
}
