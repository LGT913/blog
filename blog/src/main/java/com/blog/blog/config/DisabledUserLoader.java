package com.blog.blog.config;

import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 启动时预加载禁用用户到 Redis
 * 作用：Redis 重启后，从 DB 恢复所有禁用标记，不依赖 Redis 持久化
 */
@Configuration
public class DisabledUserLoader {

    private static final Logger log = LoggerFactory.getLogger(DisabledUserLoader.class);
    private static final String USER_DISABLED_KEY = "blog:user:disabled:";

    @Bean
    public ApplicationRunner preloadDisabledUsers(UserRepository userRepository,
                                                   RedisUtil redisUtil) {
        return args -> {
            // 从 DB 加载所有禁用用户，写入 Redis（DB 是数据真相源）
            List<Long> disabledIds = userRepository.findDisabledUserIds();
            for (Long id : disabledIds) {
                redisUtil.set(USER_DISABLED_KEY + id, "1");
            }
            if (!disabledIds.isEmpty()) {
                log.info("已从 DB 恢复 {} 个禁用用户到 Redis", disabledIds.size());
            }
        };
    }
}