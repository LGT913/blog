package com.blog.blog.controller;

import com.blog.blog.common.JwtUtil;
import com.blog.blog.common.Result;
import com.blog.blog.entity.User;
import com.blog.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
            User registeredUser=userService.register(user.getUsername(),user.getPassword(),user.getNickname());
            return Result.success(registeredUser);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody User user) {
            User loggedInUser=userService.login(user.getUsername(),user.getPassword());
            String token = jwtUtil.generateToken(loggedInUser.getId());
            Map<String, Object> data = new HashMap<>();
            data.put("user", loggedInUser);
            data.put("token", token);
            return Result.success(data);
    }
}
