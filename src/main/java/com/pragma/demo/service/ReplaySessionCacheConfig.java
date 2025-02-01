package com.pragma.demo.service;

import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ReplaySessionCacheConfig {
    @Bean
    public Cache<String, ReplaySession> sessionCache() {
        return CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES)
                .removalListener((RemovalNotification<String, ReplaySession> notification) -> {
                    log.info("Entry removed: " + notification.getKey() + " -> "
                            + notification.getValue().sessionId() + ", Reason: "
                            + notification.getCause());
                }).build();
    }
}
