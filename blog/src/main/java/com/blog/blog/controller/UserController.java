package com.blog.blog.controller;

import com.blog.blog.common.Result;
import com.blog.blog.entity.User;
import com.blog.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

//    @PostMapping("/register")
//    public User register(@RequestParam String username, @RequestParam String password, @RequestParam String nickname) {
//        return userService.register(username, password, nickname);
//    }
//
//    @PostMapping("/login")
//    public User login(@RequestParam String username, @RequestParam String password) {
//        return userService.login(username, password);
//    }
    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
            User registeredUser=userService.register(user.getUsername(),user.getPassword(),user.getNickname());
            return Result.success(registeredUser);
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody User user) {
            User loggedInUser=userService.login(user.getUsername(),user.getPassword());
            return Result.success(loggedInUser);
    }
}
