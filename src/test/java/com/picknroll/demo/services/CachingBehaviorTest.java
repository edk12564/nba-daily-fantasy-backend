package com.picknroll.demo.services;

import com.picknroll.demo.config.CacheConfig;
import com.picknroll.demo.httpclient.NbaAPIClient;
import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.models.joinTables.NbaPlayerTeam;
import com.picknroll.demo.repositories.DailyRosterRepository;
import com.picknroll.demo.repositories.DiscordChannelRepository;
import com.picknroll.demo.repositories.IsLockedRepository;
import com.picknroll.demo.repositories.NbaPlayerRepository;
import com.picknroll.demo.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = CachingBehaviorTest.Config.class)
class CachingBehaviorTest {

    @EnableCaching
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcRepositoriesAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class
    })
    @Import({
            CacheConfig.class,
            NbaPlayerServices.class,
            IsLockedServices.class,
            NbaBoxScoreService.class,
            DailyRosterServices.class
    })
    @Configuration
    static class Config {
    }

    @MockBean NbaPlayerRepository nbaPlayerRepository;
    @MockBean IsLockedRepository isLockedRepository;
    @MockBean NbaAPIClient nbaAPIClient;
    @MockBean DailyRosterRepository dailyRosterRepository;
    @MockBean DiscordChannelRepository discordChannelRepository;
    @MockBean GachaServices gachaServices;
    @MockBean JdbcTemplate jdbcTemplate;

    @Autowired NbaPlayerServices nbaPlayerServices;
    @Autowired IsLockedServices isLockedServices;
    @Autowired NbaBoxScoreService nbaBoxScoreService;
    @Autowired DailyRosterServices dailyRosterServices;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    @DisplayName("todaysPlayers cache hits repository once per date")
    void todaysPlayersCachedByDate() {
        LocalDate date = LocalDate.parse("2026-01-05");
        NbaPlayerTeam row = new NbaPlayerTeam();
        row.setPlayer_name("A");
        given(nbaPlayerRepository.getNbaPlayersWithTeam("2026-01-05")).willReturn(List.of(row));

        nbaPlayerServices.getNbaPlayersWithTeam(date);
        nbaPlayerServices.getNbaPlayersWithTeam(date);

        verify(nbaPlayerRepository, times(1)).getNbaPlayersWithTeam("2026-01-05");
    }

    @Test
    @DisplayName("lockTime cache hits repository once per date")
    void lockTimeCachedByDate() {
        LocalDate date = LocalDate.parse("2026-01-05");
        IsLocked lock = IsLocked.builder()
                .date(date)
                .lockTime(OffsetDateTime.now().plusHours(1))
                .build();
        given(isLockedRepository.isLocked(date)).willReturn(Optional.of(lock));

        isLockedServices.findLock(date);
        isLockedServices.findLock(date);

        verify(isLockedRepository, times(1)).isLocked(date);
    }

    @Test
    @DisplayName("lockTime empty results are also cached")
    void lockTimeEmptyCached() {
        LocalDate date = LocalDate.parse("2026-01-06");
        given(isLockedRepository.isLocked(date)).willReturn(Optional.empty());

        isLockedServices.findLock(date);
        isLockedServices.findLock(date);

        verify(isLockedRepository, times(1)).isLocked(date);
    }

    @Test
    @DisplayName("past scored guild leaderboard is cached")
    void pastScoredGuildLeaderboardCached() {
        LocalDate yesterday = Utils.getCaliforniaDate().minusDays(1);
        DailyRosterPlayer scored = DailyRosterPlayer.builder()
                .discordPlayerId("u1")
                .fantasyScore(40.0)
                .dollarValue(20)
                .build();
        given(dailyRosterRepository.getTodaysLeaderboardByGuildId("g1", yesterday))
                .willReturn(List.of(scored));

        dailyRosterServices.getGuildLeaderboard("g1", yesterday);
        dailyRosterServices.getGuildLeaderboard("g1", yesterday);

        verify(dailyRosterRepository, times(1)).getTodaysLeaderboardByGuildId("g1", yesterday);
    }

    @Test
    @DisplayName("past unscored guild leaderboard is not cached")
    void pastUnscoredGuildLeaderboardNotCached() {
        LocalDate yesterday = Utils.getCaliforniaDate().minusDays(1);
        DailyRosterPlayer unscored = DailyRosterPlayer.builder()
                .discordPlayerId("u1")
                .fantasyScore(null)
                .dollarValue(20)
                .build();
        given(dailyRosterRepository.getTodaysLeaderboardByGuildId("g1", yesterday))
                .willReturn(List.of(unscored));

        dailyRosterServices.getGuildLeaderboard("g1", yesterday);
        dailyRosterServices.getGuildLeaderboard("g1", yesterday);

        verify(dailyRosterRepository, times(2)).getTodaysLeaderboardByGuildId("g1", yesterday);
    }

    @Test
    @DisplayName("today guild leaderboard is never cached")
    void todayGuildLeaderboardNotCached() {
        LocalDate today = Utils.getCaliforniaDate();
        DailyRosterPlayer scored = DailyRosterPlayer.builder()
                .discordPlayerId("u1")
                .fantasyScore(40.0)
                .dollarValue(20)
                .build();
        given(dailyRosterRepository.getTodaysLeaderboardByGuildId("g1", today))
                .willReturn(List.of(scored));

        dailyRosterServices.getGuildLeaderboard("g1", today);
        dailyRosterServices.getGuildLeaderboard("g1", today);

        verify(dailyRosterRepository, times(2)).getTodaysLeaderboardByGuildId("g1", today);
    }

    @Test
    @DisplayName("past scored global and weekly leaderboards are cached")
    void pastScoredGlobalAndWeeklyCached() {
        LocalDate yesterday = Utils.getCaliforniaDate().minusDays(1);
        DailyRosterPlayer scored = DailyRosterPlayer.builder()
                .discordPlayerId("u1")
                .fantasyScore(55.0)
                .dollarValue(22)
                .build();
        given(dailyRosterRepository.getTodaysGlobalLeaderboard(yesterday)).willReturn(List.of(scored));
        given(dailyRosterRepository.getWeeksLeaderboardByGuildId(eq("g1"), any(LocalDate.class), eq(yesterday)))
                .willReturn(List.of(scored));

        dailyRosterServices.getGlobalLeaderboard(yesterday);
        dailyRosterServices.getGlobalLeaderboard(yesterday);
        dailyRosterServices.getWeeklyGuildLeaderboard("g1", yesterday);
        dailyRosterServices.getWeeklyGuildLeaderboard("g1", yesterday);

        verify(dailyRosterRepository, times(1)).getTodaysGlobalLeaderboard(yesterday);
        verify(dailyRosterRepository, times(1))
                .getWeeksLeaderboardByGuildId(eq("g1"), any(LocalDate.class), eq(yesterday));
    }

    @Test
    @DisplayName("past box scores use boxScorePast; today uses livePlayerData")
    void boxScoreRoutesByDate() {
        LocalDate yesterday = Utils.getCaliforniaDate().minusDays(1);
        LocalDate today = Utils.getCaliforniaDate();
        given(nbaAPIClient.getGamesData("past-game")).willReturn("{\"past\":true}");
        given(nbaAPIClient.getGamesData("live-game")).willReturn("{\"live\":true}");

        nbaBoxScoreService.getBoxScoreJson("past-game", yesterday);
        nbaBoxScoreService.getBoxScoreJson("past-game", yesterday);
        nbaBoxScoreService.getBoxScoreJson("live-game", today);
        nbaBoxScoreService.getBoxScoreJson("live-game", today);

        verify(nbaAPIClient, times(1)).getGamesData("past-game");
        verify(nbaAPIClient, times(1)).getGamesData("live-game");
        assertNotNull(cacheManager.getCache("boxScorePast").get("past-game"));
        assertNotNull(cacheManager.getCache("livePlayerData").get("live-game"));
        assertNull(cacheManager.getCache("livePlayerData").get("past-game"));
        assertNull(cacheManager.getCache("boxScorePast").get("live-game"));
    }
}
