package com.blog.blog.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson 企业级配置
 * 支持密码、连接池、序列化、看门狗间隔等核心参数
 *
 * 关键配置说明:
 * 1. 连接池:避免频繁创建/销毁连接,复用连接提升性能
 * 2. 看门狗:锁自动续期(leaseTime=-1 时启用),防止长任务锁过期
 * 3. 密码:支持通过环境变量配置,避免明文硬编码
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * Redis 密码(可选,通过环境变量 REDIS_PASSWORD 注入)
     * 企业级环境必须设置密码,开发环境可留空
     */
    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * 数据库编号(与 spring.data.redis.database 保持一致)
     */
    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        config.useSingleServer()
                // 1. 基础连接配置
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setDatabase(database)

                // 2. 连接池参数(企业级推荐值,可根据服务器规格调整)
                // 连接池最大数(与 lettuce max-active 对齐)
                .setConnectionPoolSize(8)
                // 最小空闲连接数(保持预热,避免峰值时创建连接开销)
                .setConnectionMinimumIdleSize(2)
                // 连接超时(与 spring.data.redis.timeout 对齐:10s)
                .setConnectTimeout(10000)
                // 命令等待超时(防止阻塞过久)
                .setTimeout(3000)
                // 连接空闲超时(释放长时间不用的连接)
                .setIdleConnectionTimeout(60000)
                // 重试次数(网络抖动时自动重试)
                .setRetryAttempts(3)
                // 重试间隔
                .setRetryInterval(1500);

        // 3. 密码配置(非空才设置,兼容开发无密码环境)
        if (StringUtils.hasText(redisPassword)) {
            config.useSingleServer().setPassword(redisPassword);
        }

        // 4. 看门狗默认续期间隔:30秒(默认值,可按需调整)
        // lock.tryLock(waitTime, -1, unit) 中 leaseTime=-1 时启用看门狗
        config.setLockWatchdogTimeout(30000);

        // 5. 传输模式:NIO(平衡性能和兼容性)
        config.setTransportMode(TransportMode.NIO);

        return Redisson.create(config);
    }
}