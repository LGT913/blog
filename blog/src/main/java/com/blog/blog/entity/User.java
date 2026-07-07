package com.blog.blog.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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

    //getter和setter
    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getUsername() {return username;}
    public void setUsername(String username) {this.username=username;}
    public String getPassword() {return password;}
    public void setPassword(String password) {this.password=password;}
    public String getNickname() {return nickname;}
    public void setNickname(String nickname) {this.nickname=nickname;}
    public LocalDateTime getCreateTime() {return createTime;}
    public void setCreateTime(LocalDateTime createTime) {this.createTime=createTime;}
}
