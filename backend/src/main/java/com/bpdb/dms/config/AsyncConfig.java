package com.bpdb.dms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for async task execution
 * Provides thread pool configuration for @Async methods
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);
    
    @Value("${spring.task.execution.pool.core-size:5}")
    private int corePoolSize;
    
    @Value("${spring.task.execution.pool.max-size:20}")
    private int maxPoolSize;
    
    @Value("${spring.task.execution.pool.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${spring.task.execution.thread-name-prefix:async-}")
    private String threadNamePrefix;
    
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        
        logger.info("Async task executor configured: coreSize={}, maxSize={}, queueCapacity={}, threadNamePrefix={}", 
                corePoolSize, maxPoolSize, queueCapacity, threadNamePrefix);
        
        return executor;
    }
}

