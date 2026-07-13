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

        if ("GET".equalsIgnoreCase(method)) {
            if (requestURI.matches("/api/article/\\d+") ||
                requestURI.matches("/api/category/\\d+") ||
                requestURI.matches("/api/comment/article/\\d+") ||
                requestURI.matches("/api/site/config")) {
                return true;
            }
        }

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

        Long userId=jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId",userId);

        return true;
    }
}
