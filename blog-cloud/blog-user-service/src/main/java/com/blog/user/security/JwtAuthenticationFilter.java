package com.blog.user.security;

import com.blog.user.util.RedisUtil;
import com.blog.user.entity.User;
import com.blog.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final UserRepository userRepository;

    private static final String TOKEN_BLACKLIST_KEY = "blog:jwt:blacklist:";
    private static final String USER_DISABLED_KEY = "blog:user:disabled:";

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   RedisUtil redisUtil,
                                   UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
//        // 新增白名单跳过逻辑
//        String uri = request.getRequestURI();
//        String method = request.getMethod();
//        // 无需Token校验的接口直接放行，跳过所有JWT校验
//        boolean isSkipPath = uri.equals("/api/user/login")
//                || uri.equals("/api/user/register")
//                || uri.startsWith("/api/user/internal/")
//                || "OPTIONS".equals(method);
//        if (isSkipPath) {
//            filterChain.doFilter(request, response);
//            return;
//        }

        String token = request.getHeader("Authorization");

        //Bearer前缀处理
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        // 1. 没有 token → 直接放行，交给 Security 判断是否需要登录
        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. token 无效 → 清除上下文，放行
        if (!jwtUtil.validateToken(token)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // 3. token 有效 → 解析载荷
        String jti = jwtUtil.getJtiFromToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        // 检查 1：Token 黑名单（用 jti，零碰撞）
        // Redis 不可用时按"不在黑名单"处理（降级放行），避免过滤器异常导致整条请求失败
        boolean inBlacklist = false;
        try {
            inBlacklist = Boolean.TRUE.equals(redisUtil.hasKey(TOKEN_BLACKLIST_KEY + jti));
        } catch (Exception e) {
            log.warn("检查 token 黑名单失败（Redis 不可用？），按不在黑名单处理, jti={}", jti, e);
        }
        if (inBlacklist) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        //  检查 2：用户禁用标记（Redis + DB 降级兜底）
        if (isUserDisabled(userId)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // 4. 全部通过 → 设置认证上下文
        UserPrincipal principal = new UserPrincipal(userId, role);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 5. 放行
        filterChain.doFilter(request, response);
    }

    /**
     * 检查用户是否被禁用
     * 优先查 Redis（0.1ms），Redis 说禁用时交叉校验 DB（防止 Redis 脏数据误判）
     * Redis 不可用时降级查 DB 兜底
     */
    private boolean isUserDisabled(Long userId) {
        try {
            // 正常路径：查 Redis 缓存，0.1ms
            boolean redisDisabled = Boolean.TRUE.equals(redisUtil.hasKey(USER_DISABLED_KEY + userId));
            if (!redisDisabled) {
                return false;
            }
            // Redis 说禁用 → 交叉校验 DB，防止 Redis 脏数据（如 enableUser 只写了 DB 但 Redis 没删干净）
            Optional<User> userOpt = userRepository.findById(userId);
            boolean dbDisabled = userOpt.map(u -> Boolean.FALSE.equals(u.getEnabled())).orElse(false);
            if (!dbDisabled) {
                // DB 已恢复但 Redis 残留脏 key → 清理 Redis，避免持续误判
                redisUtil.del(USER_DISABLED_KEY + userId);
                log.warn("Redis 禁用标记与 DB 不一致，已清理，userId={}", userId);
                return false;
            }
            return true;
        } catch (Exception e) {
            // Redis 不可用（任何异常）→ 降级查 DB 兜底
            try {
                Optional<User> userOpt = userRepository.findById(userId);
                return userOpt.map(u -> Boolean.FALSE.equals(u.getEnabled())).orElse(false);
            } catch (Exception dbEx) {
                // DB 也不可用 → 放行（避免 Redis 故障导致全站瘫痪）
                log.error("Redis 和 DB 均不可用，降级放行，userId={}", userId, dbEx);
                return false;
            }
        }
    }
}