package com.libowen.searcher.indexer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {
    @Bean
    public ExecutorService executorService(){
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                8,
                20,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5000),
                (Runnable task) -> {
                    Thread thread = new Thread(task);
                    thread.setName("批量插入线程");
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
        return threadPoolExecutor;
    }
}
