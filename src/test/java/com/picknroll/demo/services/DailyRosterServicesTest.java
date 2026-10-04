package com.picknroll.demo.services;

import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.repositories.DailyRosterRepository;
import com.picknroll.demo.repositories.DiscordChannelRepository;
import com.picknroll.demo.services.RosterWriteOutcome.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyRosterServicesTest {

    @Mock
    private DailyRosterRepository dailyRosterRepository;

    @Mock
    private DiscordChannelRepository discordChannelRepository;

    @Mock
    private IsLockedServices isLockedServices;

    @Mock
    private JdbcTemplate jdbcTemplate;

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
        DailyRosterPlayer p = openPlayer("u1");
        givenOpen(date);
        RosterWriteOutcome outcome = services.deleteRosterPlayerIfOpen(p);
        assertEquals(Status.DELETED, outcome.status());
        verify(dailyRosterRepository).deleteRosterPlayerByDateAndDiscordIdAndPlayerName(date, "u1", p.getNbaPlayerUid());
    }

    @Test
    @DisplayName("save and delete reject a missing lock without writing")
    void mutationsRejectMissingLock() {
        given(isLockedServices.findLock(date)).willReturn(Optional.empty());
        UUID uid = UUID.randomUUID();

        RosterWriteOutcome saved = services.saveRosterChoiceWithinCap(uid, "u1", "nick", "PG", date, 100);
        RosterWriteOutcome deleted = services.deleteRosterPlayerIfOpen(openPlayer("u1"));

        assertEquals(Status.NO_GAMES, saved.status());
        assertEquals(Status.NO_GAMES, deleted.status());
        verify(dailyRosterRepository, never()).saveRosterChoice(any(), any(), any(), any(), any());
        verify(dailyRosterRepository, never()).deleteRosterPlayerByDateAndDiscordIdAndPlayerName(any(), any(), any());
    }

    @Test
    @DisplayName("save and delete reject a lock time that has already passed")
    void mutationsRejectPastLock() {
        IsLocked past = IsLocked.builder().date(date).lockTime(OffsetDateTime.now().minusMinutes(1)).build();
        given(isLockedServices.findLock(date)).willReturn(Optional.of(past));

        RosterWriteOutcome saved = services.saveRosterChoiceWithinCap(UUID.randomUUID(), "u1", "nick", "PG", date, 100);
        RosterWriteOutcome deleted = services.deleteRosterPlayerIfOpen(openPlayer("u1"));

        assertEquals(Status.LOCKED, saved.status());
        assertEquals(Status.LOCKED, deleted.status());
        verify(dailyRosterRepository, never()).saveRosterChoice(any(), any(), any(), any(), any());
        verify(dailyRosterRepository, never()).deleteRosterPlayerByDateAndDiscordIdAndPlayerName(any(), any(), any());
    }

    @Test
    @DisplayName("save writes at the cap and rejects one dollar over it")
    void saveRespectsCapBoundary() {
        givenOpen(date);
        UUID uid = UUID.randomUUID();
        given(dailyRosterRepository.getTodaysRosterPriceWithPlayer(eq("u1"), eq("PG"), eq(date.toString()), eq(uid)))
                .willReturn(List.of(100));
        given(dailyRosterRepository.saveRosterChoice(uid, "u1", "nick", "PG", date.toString())).willReturn(1);

        assertEquals(Status.SAVED, services.saveRosterChoiceWithinCap(uid, "u1", "nick", "PG", date, 100).status());

        given(dailyRosterRepository.getTodaysRosterPriceWithPlayer(eq("u1"), eq("SG"), eq(date.toString()), eq(uid)))
                .willReturn(List.of(101));
        RosterWriteOutcome over = services.saveRosterChoiceWithinCap(uid, "u1", "nick", "SG", date, 100);
        assertEquals(Status.OVER_CAP, over.status());
        assertEquals(101, over.totalPrice());
        verify(dailyRosterRepository, never()).saveRosterChoice(uid, "u1", "nick", "SG", date.toString());
    }

    @Test
    @DisplayName("getWeeklyGuildLeaderboard includes Monday through the requested day")
    void testWeeklyLeaderboard() {
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        given(dailyRosterRepository.getWeeksLeaderboardByGuildId("g1", weekStart, date)).willReturn(List.of());
        List<DailyRosterPlayer> list = services.getWeeklyGuildLeaderboard("g1", date);
        assertNotNull(list);
        verify(dailyRosterRepository).getWeeksLeaderboardByGuildId("g1", weekStart, date);
    }

    @Test
    @DisplayName("weekly window on Monday is that Monday only")
    void weeklyWindowOnMondayIncludesThatMonday() {
        LocalDate monday = LocalDate.parse("2025-12-22");
        given(dailyRosterRepository.getWeeksLeaderboardByGuildId("g1", monday, monday)).willReturn(List.of());
        services.getWeeklyGuildLeaderboard("g1", monday);
        verify(dailyRosterRepository).getWeeksLeaderboardByGuildId("g1", monday, monday);
    }

    @Test
    @DisplayName("weekly window on Sunday runs from that week's Monday through Sunday")
    void weeklyWindowOnSundayIncludesMondayAndSunday() {
        LocalDate sunday = LocalDate.parse("2025-12-28");
        LocalDate monday = LocalDate.parse("2025-12-22");
        given(dailyRosterRepository.getWeeksLeaderboardByGuildId("g1", monday, sunday)).willReturn(List.of());
        services.getWeeklyGuildLeaderboard("g1", sunday);
        verify(dailyRosterRepository).getWeeksLeaderboardByGuildId("g1", monday, sunday);
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

    private void givenOpen(LocalDate lockDate) {
        given(isLockedServices.findLock(lockDate)).willReturn(Optional.of(
                IsLocked.builder().date(lockDate).lockTime(OffsetDateTime.now().plusHours(1)).build()));
        given(jdbcTemplate.execute(anyString(), any(PreparedStatementCallback.class))).willReturn(null);
    }

    private DailyRosterPlayer openPlayer(String discordId) {
        return DailyRosterPlayer.builder()
                .discordPlayerId(discordId)
                .nbaPlayerUid(UUID.randomUUID())
                .date(date)
                .build();
    }
}
