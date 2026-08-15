package com.blog.user.repository;

import com.blog.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    //根据用户名查找用户
    Optional<User> findByUsername(String username);

    // 查询所有被禁用的用户 ID（启动时预加载 Redis 用）
    @Query("SELECT u.id FROM User u WHERE u.enabled = false")
    List<Long> findDisabledUserIds();
}