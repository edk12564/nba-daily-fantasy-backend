package com.picknroll.demo.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DiscordPlayerGuildRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private DiscordPlayerGuildRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertGuildForPlayerId_isIdempotent() {
        String player = "p-test-1";
        String guild = "g-test-1";

        repository.insertGuildForPlayerId(player, guild);
        repository.insertGuildForPlayerId(player, guild); // should not duplicate due to ON CONFLICT DO NOTHING

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM discord_player_guilds WHERE discord_player_id=? AND guild_id=?",
                Integer.class, player, guild);
        assertEquals(1, count);
    }
}
