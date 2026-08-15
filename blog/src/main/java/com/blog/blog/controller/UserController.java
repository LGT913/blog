package com.blog.blog.controller;

import com.blog.blog.common.util.JwtUtil;
import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.common.result.Result;
import com.blog.blog.entity.User;
import com.blog.blog.repository.UserRepository;
import com.blog.blog.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final String TOKEN_BLACKLIST_KEY = "blog:jwt:blacklist:";
    private static final String USER_DISABLED_KEY = "blog:user:disabled:";

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
            User registeredUser=userService.register(user.getUsername(),user.getPassword(),user.getNickname());
            return Result.success(registeredUser);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody User loginRequest) {
        User loggedInUser = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
        String token = jwtUtil.generateToken(loggedInUser.getId(), loggedInUser.getRole().name());
        Map<String, Object> data = new HashMap<>();
        data.put("user", loggedInUser);
        data.put("token", token);
        return Result.success(data);
    }

    // ==================== 新增：登出 / 踢下线 ====================

    // 登出：当前 token 加入黑名单（TTL = token 剩余时间，到期自动清除）
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        if (token == null || token.isEmpty()) {
            return Result.success("未登录");
        }
        String jti = jwtUtil.getJtiFromToken(token);
        long remainSeconds = jwtUtil.getRemainingSeconds(token);
        if (remainSeconds > 0) {
            redisUtil.set(TOKEN_BLACKLIST_KEY + jti, "1", remainSeconds);
        }
        return Result.success("登出成功");
    }

    // 管理员踢人：DB + Redis 双写（先 DB 后 Redis，Redis 失败不阻断 DB 操作）
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/kick/{userId}")
    public Result<String> kickUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        // 第一步：DB 落地（真相源，Redis 重启后从此恢复）
        user.setEnabled(false);
        userRepository.save(user);
        // 第二步：写入 Redis（加速认证，Filter 只查 Redis 不查 DB）
        try {
            redisUtil.set(USER_DISABLED_KEY + userId, "1");
        } catch (Exception e) {
            // Redis 写入失败不阻断 DB 操作，启动时 DisabledUserLoader 会从 DB 恢复
            log.error("Redis 写入禁用标记失败，用户ID：{}，启动时将自动恢复", userId, e);
        }
        return Result.success("用户已下线");
    }

    // 管理员恢复账号：DB + Redis 双删
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/enable/{userId}")
    public Result<String> enableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setEnabled(true);
        userRepository.save(user);
        try {
            redisUtil.del(USER_DISABLED_KEY + userId);
        } catch (Exception e) {
            log.error("Redis 删除禁用标记失败，用户ID：{}", userId, e);
        }
        return Result.success("账号已恢复");
    }
}