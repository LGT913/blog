package com.blog.blog.service.impl;

import com.blog.blog.entity.User;
import com.blog.blog.repository.UserRepository;
import com.blog.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public User register(String username,String password,String nickname){
        Optional<User> existingUser = userRepository.findByUsername(username);
        if(existingUser.isPresent()){
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setRole(User.Role.USER);   // 新注册都是普通用户
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setCreateTime(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public User login(String username,String password){
        User user=userRepository.findByUsername(username).orElseThrow(()->new RuntimeException("用户不存在"));
        //用加密工具比对密码
        if(!passwordEncoder.matches(password,user.getPassword())){
            throw new RuntimeException("密码错误");
        }
        return user;
    }
}