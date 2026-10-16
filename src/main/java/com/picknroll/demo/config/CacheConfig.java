package com.picknroll.demo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                new CaffeineCache(
                        "livePlayerData",
                        Caffeine.newBuilder()
                                .maximumSize(1000)
                                .expireAfterWrite(60, TimeUnit.SECONDS)
                                .build()),
                new CaffeineCache(
                        "nbaSchedule",
                        Caffeine.newBuilder()
                                .maximumSize(2)
                                .expireAfterWrite(24, TimeUnit.HOURS)
                                .build())));
        return manager;
    }
}
