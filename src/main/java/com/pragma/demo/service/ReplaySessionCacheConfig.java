package com.pragma.demo.service;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ReplaySessionCacheConfig {

    @Value("${cache.expiry.duration}")
    private long expiryDuration;

    @Value("${cache.expiry.unit}")
    private TimeUnit expiryUnit;
    
    @Bean
    public Cache<String, ReplaySession> sessionCache() {
        log.info("Creating cache with expiry duration: {expiryDuration} {expiryUnit}");
        return CacheBuilder.newBuilder().expireAfterAccess(expiryDuration, expiryUnit)
                .removalListener((RemovalNotification<String, ReplaySession> notification) -> {
                    log.trace("Entry removed: " + notification.getKey() + " -> "
                            + notification.getValue().sessionId() + ", Reason: "
                            + notification.getCause());
                }).build();
    }
}
