package com.blog.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GatewayGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long start = System.currentTimeMillis();
        log.info("[网关] 请求: {} {}", request.getMethod(), request.getURI());
        // 示例：给下游透传网关标识头
        ServerHttpRequest mutated = request.mutate()
                .header("X-Gateway", "blog-gateway")
                .build();
        return chain.filter(exchange.mutate().request(mutated).build())
                .then(Mono.fromRunnable(() ->
                        log.info("[网关] 完成: {} 耗时 {}ms", request.getURI(), System.currentTimeMillis() - start)));
    }

    @Override
    public int getOrder() {
        return 0; // 数值越小优先级越高
    }
}