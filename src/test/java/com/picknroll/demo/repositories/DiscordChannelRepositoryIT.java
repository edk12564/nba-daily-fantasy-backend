package com.picknroll.demo.repositories;

import com.picknroll.demo.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DiscordChannelRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private DiscordChannelRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertChannelForDate_upsertsByPrimaryKey() {
        String channel = "ch-test-1";
        String guild = "g-test-1";
        LocalDate date = LocalDate.parse("2025-12-26");

        repository.insertChannelForDate(channel, date, guild);
        repository.insertChannelForDate(channel, date, guild); // should update created_at but keep single row

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM discord_channels WHERE channel_id=? AND date=?",
                Integer.class, channel, date);
        assertEquals(1, count);
    }
}
