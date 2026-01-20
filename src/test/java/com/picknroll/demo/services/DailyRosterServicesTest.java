package com.picknroll.demo.services;

import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.repositories.DailyRosterRepository;
import com.picknroll.demo.repositories.DiscordChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyRosterServicesTest {

    @Mock
    private DailyRosterRepository dailyRosterRepository;

    @Mock
    private DiscordChannelRepository discordChannelRepository;

    @InjectMocks
    private DailyRosterServices services;

    private LocalDate date;

    @BeforeEach
    void setUp() {
        date = LocalDate.parse("2025-12-25");
    }

    @Test
    @DisplayName("getTodaysRosterPriceWithPlayer sums repository parts and passes args correctly")
    void testPriceSum() {
        given(dailyRosterRepository.getTodaysRosterPriceWithPlayer(eq("user1"), eq("SF"), eq(date.toString()), any(UUID.class)))
                .willReturn(List.of(10, 20, 30));
        int total = services.getTodaysRosterPriceWithPlayer("user1", "SF", date, UUID.randomUUID());
        assertEquals(60, total);
    }

    @Test
    @DisplayName("saveRosterChoice forwards to repository with string date")
    void testSaveRosterChoice() {
        UUID uid = UUID.randomUUID();
        given(dailyRosterRepository.saveRosterChoice(uid, "u1", "nick", "PG", date.toString())).willReturn(1);
        Integer changed = services.saveRosterChoice(uid, "u1", "nick", "PG", date);
        assertEquals(1, changed);
    }

    @Test
    @DisplayName("deleteRosterPlayer forwards to repository")
    void testDeleteRosterPlayer() {
        DailyRosterPlayer p = DailyRosterPlayer.builder()
                .discordPlayerId("u1")
                .nbaPlayerUid(UUID.randomUUID())
                .date(date)
                .build();
        services.deleteRosterPlayer(p);
        verify(dailyRosterRepository).deleteRosterPlayerByDateAndDiscordIdAndPlayerName(date, "u1", p.getNbaPlayerUid());
    }

    @Test
    @DisplayName("getWeeklyGuildLeaderboard computes previous Monday window")
    void testWeeklyLeaderboard() {
        LocalDate previousMonday = date.with(java.time.temporal.TemporalAdjusters.previous(DayOfWeek.MONDAY));
        given(dailyRosterRepository.getWeeksLeaderboardByGuildId("g1", previousMonday, date)).willReturn(List.of());
        List<DailyRosterPlayer> list = services.getWeeklyGuildLeaderboard("g1", date);
        assertNotNull(list);
        verify(dailyRosterRepository).getWeeksLeaderboardByGuildId("g1", previousMonday, date);
    }

    @Test
    @DisplayName("getPlayerRoster returns roster from repository")
    void testGetPlayerRoster() {
        DailyRosterPlayer player = DailyRosterPlayer.builder()
                .name("LeBron James")
                .discordPlayerId("u1")
                .build();
        given(dailyRosterRepository.getTodaysRosterByDiscordId("u1", date)).willReturn(List.of(player));
        List<DailyRosterPlayer> roster = services.getPlayerRoster("u1", date);
        assertEquals(1, roster.size());
        assertEquals("LeBron James", roster.get(0).getName());
    }

    @Test
    @DisplayName("getGlobalLeaderboard returns leaderboard from repository")
    void testGetGlobalLeaderboard() {
        DailyRosterPlayer player = DailyRosterPlayer.builder()
                .name("LeBron James")
                .build();
        given(dailyRosterRepository.getTodaysGlobalLeaderboard(date)).willReturn(List.of(player));
        List<DailyRosterPlayer> leaderboard = services.getGlobalLeaderboard(date);
        assertEquals(1, leaderboard.size());
    }

    @Test
    @DisplayName("getGuildLeaderboard returns leaderboard from repository")
    void testGetGuildLeaderboard() {
        DailyRosterPlayer player = DailyRosterPlayer.builder()
                .name("LeBron James")
                .build();
        given(dailyRosterRepository.getTodaysLeaderboardByGuildId("guild-1", date)).willReturn(List.of(player));
        List<DailyRosterPlayer> leaderboard = services.getGuildLeaderboard("guild-1", date);
        assertEquals(1, leaderboard.size());
    }
}
