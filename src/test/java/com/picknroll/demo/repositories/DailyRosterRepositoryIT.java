package com.picknroll.demo.repositories;

import com.picknroll.demo.config.AbstractIntegrationTest;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DailyRosterRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private DailyRosterRepository dailyRosterRepository;

    @Autowired
    private DiscordPlayerGuildRepository discordPlayerGuildRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final LocalDate TEST_DATE = LocalDate.parse("2025-12-25");

    @Test
    void saveGetDeleteRosterFlow_andPriceCalc() {
        String discordId = "user-test-1";
        // Insert two roster choices for different positions
        UUID curry = UUID.fromString("22222222-2222-2222-2222-222222222222"); // PG 11500
        UUID lebron = UUID.fromString("11111111-1111-1111-1111-111111111111"); // SF 12000

        Integer changed1 = dailyRosterRepository.saveRosterChoice(curry, discordId, "nick1", "PG", TEST_DATE.toString());
        assertEquals(1, changed1);
        Integer changed2 = dailyRosterRepository.saveRosterChoice(lebron, discordId, "nick2", "SF", TEST_DATE.toString());
        assertEquals(1, changed2);

        // Upsert same position with new nickname should update
        Integer changed3 = dailyRosterRepository.saveRosterChoice(lebron, discordId, "King", "SF", TEST_DATE.toString());
        assertEquals(1, changed3);

        List<DailyRosterPlayer> roster = dailyRosterRepository.getTodaysRosterByDiscordId(discordId, TEST_DATE);
        assertEquals(2, roster.size());

        // Price calc with adding Jokic should include existing different positions + incoming player's price
        UUID jokic = UUID.fromString("33333333-3333-3333-3333-333333333333"); // C 13000
        List<Integer> parts = dailyRosterRepository.getTodaysRosterPriceWithPlayer(discordId, "C", TEST_DATE.toString(), jokic);
        assertTrue(parts.size() >= 1);
        int sum = parts.stream().reduce(0, Integer::sum);
        assertTrue(sum >= 11500 + 12000); // at least existing PG and SF

        // Delete one entry
        dailyRosterRepository.deleteRosterPlayerByDateAndDiscordIdAndPlayerName(TEST_DATE, discordId, lebron);
        List<DailyRosterPlayer> afterDelete = dailyRosterRepository.getTodaysRosterByDiscordId(discordId, TEST_DATE);
        assertEquals(1, afterDelete.size());
    }

    @Test
    void leaderboardsQueries_workWithGuilds() {
        String guildId = "guild-test-1";
        String userA = "userA-test";
        String userB = "userB-test";
        discordPlayerGuildRepository.insertGuildForPlayerId(userA, guildId);
        discordPlayerGuildRepository.insertGuildForPlayerId(userB, guildId);

        UUID curry = UUID.fromString("22222222-2222-2222-2222-222222222222"); // 48.2
        UUID lebron = UUID.fromString("11111111-1111-1111-1111-111111111111"); // 55.5

        // Seed daily_roster rows (date must match players' string date)
        dailyRosterRepository.saveRosterChoice(curry, userA, "A1", "PG", TEST_DATE.toString());
        dailyRosterRepository.saveRosterChoice(lebron, userA, "A2", "SF", TEST_DATE.toString());
        dailyRosterRepository.saveRosterChoice(curry, userB, "B1", "PG", TEST_DATE.toString());

        // Global leaderboard
        List<DailyRosterPlayer> global = dailyRosterRepository.getTodaysGlobalLeaderboard(TEST_DATE);
        assertFalse(global.isEmpty());

        // Guild leaderboard for the date
        List<DailyRosterPlayer> dailyGuild = dailyRosterRepository.getTodaysLeaderboardByGuildId(guildId, TEST_DATE);
        assertFalse(dailyGuild.isEmpty());

        // Weekly leaderboard: create earlier-day row within a week window
        LocalDate earlier = TEST_DATE.minusDays(2);
        jdbcTemplate.update("INSERT INTO daily_roster(discord_player_id, nba_player_uid, date, nickname, position) VALUES (?,?,?,?,?::daily_roster_position) ON CONFLICT DO NOTHING",
                userA, curry, earlier, "A0", "SG");
        // ensure discord_player_guilds already has mapping
        List<DailyRosterPlayer> weekly = dailyRosterRepository.getWeeksLeaderboardByGuildId(guildId, earlier.minusDays(1), TEST_DATE.plusDays(1));
        assertFalse(weekly.isEmpty());
    }
}
