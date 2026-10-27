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
                        "boxScorePast",
                        Caffeine.newBuilder()
                                .maximumSize(500)
                                // Finals for past Pacific dates — refresh far less often.
                                .expireAfterWrite(6, TimeUnit.HOURS)
                                .build()),
                new CaffeineCache(
                        "nbaSchedule",
                        Caffeine.newBuilder()
                                .maximumSize(2)
                                .expireAfterWrite(24, TimeUnit.HOURS)
                                .build()),
                new CaffeineCache(
                        "gachaWeightedPool",
                        Caffeine.newBuilder()
                                .maximumSize(2)
                                // Pool is keyed by Pacific date; loader usually runs once/day.
                                .expireAfterWrite(6, TimeUnit.HOURS)
                                .build()),
                new CaffeineCache(
                        "todaysPlayers",
                        Caffeine.newBuilder()
                                .maximumSize(8)
                                .expireAfterWrite(10, TimeUnit.MINUTES)
                                .build()),
                new CaffeineCache(
                        "lockTime",
                        Caffeine.newBuilder()
                                .maximumSize(16)
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .build()),
                new CaffeineCache(
                        "leaderboardPast",
                        Caffeine.newBuilder()
                                .maximumSize(500)
                                // Only used when date < Pacific today (see DailyRosterServices).
                                .expireAfterWrite(6, TimeUnit.HOURS)
                                .build())));
        return manager;
    }
}
