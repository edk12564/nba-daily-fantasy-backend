package com.picknroll.demo.repositories;

import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.services.DailyRosterServices;
import com.picknroll.demo.services.RosterWriteOutcome;
import com.picknroll.demo.services.RosterWriteOutcome.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DailyRosterRepositoryIT extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void capRacePool(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.hikari.maximumPoolSize", () -> "5");
    }

    @Autowired
    private DailyRosterRepository dailyRosterRepository;

    @Autowired
    private DiscordPlayerGuildRepository discordPlayerGuildRepository;

    @Autowired
    private DailyRosterServices dailyRosterServices;

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

    @Test
    void weeklyLeaderboardIncludesMondayThroughRequestedDay() {
        String guildId = "guild-week-bounds";
        String user = "week-user";
        discordPlayerGuildRepository.insertGuildForPlayerId(user, guildId);

        LocalDate previousMonday = LocalDate.parse("2025-12-15");
        LocalDate monday = LocalDate.parse("2025-12-22");
        LocalDate tuesday = LocalDate.parse("2025-12-23");
        LocalDate sunday = LocalDate.parse("2025-12-28");
        insertScoredRoster(user, UUID.fromString("cccccccc-cccc-cccc-cccc-ccccccccccc1"), 9301, previousMonday, 100);
        insertScoredRoster(user, UUID.fromString("cccccccc-cccc-cccc-cccc-ccccccccccc2"), 9302, monday, 10);
        insertScoredRoster(user, UUID.fromString("cccccccc-cccc-cccc-cccc-ccccccccccc3"), 9303, tuesday, 20);
        insertScoredRoster(user, UUID.fromString("cccccccc-cccc-cccc-cccc-ccccccccccc4"), 9304, sunday, 40);

        assertEquals(10.0, weeklyScore(guildId, monday), 0.001);
        assertEquals(70.0, weeklyScore(guildId, sunday), 0.001);
    }

    @Test
    void saveAndDeleteFailClosedWhenLockRowIsMissing() {
        LocalDate date = LocalDate.parse("2026-10-21");
        String user = "nolock-user";
        UUID player = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
        insertPlayer(player, 9401, "No Lock", date.toString(), "PG", 10, 1);
        insertRoster(user, player, date, "nl", "PG");

        RosterWriteOutcome saved = dailyRosterServices.saveRosterChoiceWithinCap(
                player, user, "nl", "PG", date, 100);
        DailyRosterPlayer rosterPlayer = DailyRosterPlayer.builder()
                .discordPlayerId(user)
                .nbaPlayerUid(player)
                .date(date)
                .build();
        RosterWriteOutcome deleted = dailyRosterServices.deleteRosterPlayerIfOpen(rosterPlayer);

        assertEquals(Status.NO_GAMES, saved.status());
        assertEquals(Status.NO_GAMES, deleted.status());
        assertEquals(1, rosterCount(user, date));
    }

    @Test
    void concurrentRosterWritesCannotExceedCap() throws Exception {
        LocalDate date = LocalDate.parse("2026-10-20");
        String user = "cap-user";
        UUID pointGuard = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
        UUID shootingGuard = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
        insertPlayer(pointGuard, 9201, "Cap PG", date.toString(), "PG", 60, 1);
        insertPlayer(shootingGuard, 9202, "Cap SG", date.toString(), "SG", 60, 1);
        jdbcTemplate.update(
                "INSERT INTO is_locked (date, lock_time) VALUES (?, ?) ON CONFLICT (date) DO UPDATE SET lock_time = EXCLUDED.lock_time",
                date, OffsetDateTime.now().plusHours(3));

        DataSource dataSource = jdbcTemplate.getDataSource();
        assertNotNull(dataSource);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        boolean blocked = false;
        RosterWriteOutcome outcome;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            holdRosterLockAndInsert(connection, user, pointGuard, date);
            Future<RosterWriteOutcome> pending = executor.submit(() -> {
                started.countDown();
                return dailyRosterServices.saveRosterChoiceWithinCap(
                        shootingGuard, user, "sg", "SG", date, 100);
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            blocked = waitForUngrantedAdvisoryLock(connection);
            if (blocked) {
                connection.commit();
            } else {
                connection.rollback();
            }
            outcome = pending.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }

        assertTrue(blocked, "second roster write should wait on the player/day lock");
        assertEquals(Status.OVER_CAP, outcome.status());
        assertEquals(120, outcome.totalPrice());
        assertEquals(1, rosterCount(user, date));
    }

    private double weeklyScore(String guildId, LocalDate date) {
        List<DailyRosterPlayer> rows = dailyRosterServices.getWeeklyGuildLeaderboard(guildId, date);
        assertEquals(1, rows.size());
        return rows.get(0).getFantasyScore();
    }

    private void insertScoredRoster(String discordId, UUID playerId, int nbaPlayerId, LocalDate date, double score) {
        insertPlayer(playerId, nbaPlayerId, "Week " + date, date.toString(), "PG", 10, score);
        insertRoster(discordId, playerId, date, "week", "PG");
    }

    private void insertPlayer(UUID playerId, int nbaPlayerId, String name, String date, String position,
                              int dollars, double score) {
        jdbcTemplate.update("""
                INSERT INTO nba_players (
                    nba_player_uid, nba_player_id, name, date, position, dollar_value, fantasy_score, team_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                """, playerId, nbaPlayerId, name, date, position, dollars, score);
    }

    private void insertRoster(String discordId, UUID playerId, LocalDate date, String nickname, String position) {
        jdbcTemplate.update("""
                INSERT INTO daily_roster (discord_player_id, nba_player_uid, date, nickname, position)
                VALUES (?, ?, ?, ?, ?::daily_roster_position)
                """, discordId, playerId, date, nickname, position);
    }

    private int rosterCount(String discordId, LocalDate date) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM daily_roster WHERE discord_player_id = ? AND date = ?",
                Integer.class, discordId, date);
        return count == null ? 0 : count;
    }

    private void holdRosterLockAndInsert(Connection connection, String discordId, UUID playerId, LocalDate date)
            throws Exception {
        try (PreparedStatement lock = connection.prepareStatement(
                "SELECT pg_advisory_xact_lock(hashtext(?), hashtext(?))")) {
            lock.setString(1, discordId);
            lock.setString(2, date.toString());
            lock.execute();
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO daily_roster (discord_player_id, nba_player_uid, date, nickname, position)
                VALUES (?, ?, ?, ?, ?::daily_roster_position)
                """)) {
            insert.setString(1, discordId);
            insert.setObject(2, playerId);
            insert.setObject(3, date);
            insert.setString(4, "pg");
            insert.setString(5, "PG");
            insert.executeUpdate();
        }
    }

    private boolean waitForUngrantedAdvisoryLock(Connection connection) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            try (PreparedStatement waiting = connection.prepareStatement(
                    "SELECT COUNT(*) FROM pg_locks WHERE locktype = 'advisory' AND NOT granted")) {
                try (ResultSet results = waiting.executeQuery()) {
                    if (results.next() && results.getInt(1) > 0) {
                        return true;
                    }
                }
            }
            Thread.sleep(50);
        }
        return false;
    }
}
