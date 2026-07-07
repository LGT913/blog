package com.blog.blog.service;

import com.blog.blog.entity.User;

public interface UserService {
    User register(String username, String password, String nickname);
    User login(String username,String password);
}
