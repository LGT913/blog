package com.blog.user.service.impl;

import com.blog.user.entity.User;
import com.blog.user.repository.UserRepository;
import com.blog.user.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(String username, String password, String nickname) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setCreateTime(LocalDateTime.now());
        user.setEnabled(true);  // 注册时默认启用
        return userRepository.save(user);
    }

    @Override
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        // 账号被禁用 → 拒绝登录（只有显式 false 才拒绝；enabled 为 null 的旧数据视为启用，防止误判）
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }
        return user;
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}