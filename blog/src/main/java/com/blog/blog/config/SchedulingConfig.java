package com.blog.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * 定时任务线程池
     * 确保 syncViewCountToDB 等 @Scheduled 任务有独立的线程池,不会阻塞其他任务
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setErrorHandler(throwable -> {
            // 定时任务异常兜底,避免吞掉异常
            org.slf4j.LoggerFactory.getLogger(SchedulingConfig.class)
                    .error("定时任务执行异常", throwable);
        });
        return scheduler;
    }
}