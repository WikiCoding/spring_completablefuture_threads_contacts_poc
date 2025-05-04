package com.wikicoding.asynccrudcompletablefuture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class AsynccrudcompletablefutureApplication {
    private static final int threadCount = 10;

    public static void main(String[] args) {
        SpringApplication.run(AsynccrudcompletablefutureApplication.class, args);
    }

    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);
        executor.setMaxPoolSize(threadCount);
        executor.setQueueCapacity(threadCount);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
