package com.blog.user.service;

import com.blog.user.entity.User;

public interface UserService {
    User register(String username, String password, String nickname);
    User login(String username, String password);
    User getById(Long id);
}
