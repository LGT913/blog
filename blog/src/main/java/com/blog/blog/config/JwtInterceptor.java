package com.blog.blog.config;

import com.blog.blog.common.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 1. 放行 OPTIONS 预检请求（跨域）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 2. 放行公开的 GET 请求（文章详情、分类详情等）
        if ("GET".equalsIgnoreCase(method)) {
            if (requestURI.matches("/api/article/\\d+") ||
                requestURI.matches("/api/category/\\d+") ||
                requestURI.matches("/api/comment/article/\\d+") ||
                requestURI.equals("/api/site/config") ||
                requestURI.equals("/api/article/list") ||
                requestURI.equals("/api/article/ranking/views") ||
                requestURI.equals("/api/article/ranking/latest") ||
                requestURI.equals("/api/category/list") ||
                requestURI.equals("/api/notice/list")) {
                return true;
            }
        }

        // 3. 检查 token
        String token = request.getHeader("Authorization");
        if(token==null||token.isEmpty()){
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\",\"data\":null}");
            return false;
        }
        if(!jwtUtil.validateToken(token)){
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"message\":\"token无效或已过期\",\"data\":null}");
            return false;
        }

        // 4. token 有效，提取用户 ID 放入请求属性
        Long userId=jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId",userId);

        return true;
    }
}
