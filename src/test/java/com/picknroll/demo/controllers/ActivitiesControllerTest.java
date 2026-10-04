package com.picknroll.demo.controllers;

import com.picknroll.demo.httpclient.NbaAPIClient;
import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.models.joinTables.NbaPlayerTeam;
import com.picknroll.demo.services.*;
import com.picknroll.demo.services.RosterWriteOutcome.Status;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    classes = ActivitiesControllerTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "discord.client.id=test-client-id",
        "discord.client.secret=test-secret",
        "jwt.secret=test-secret-key-for-jwt-signing-minimum-32-characters"
})
class ActivitiesControllerTest {

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final String AUTH_HEADER = "Bearer " + TEST_TOKEN;

    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        JdbcRepositoriesAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
    })
    @ComponentScan(
        basePackages = {"com.picknroll.demo.controllers", "com.picknroll.demo.interceptors"},
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Repository.*")
    )
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NbaPlayerServices nbaPlayerServices;
    @MockBean
    private DailyRosterServices dailyRosterServices;
    @MockBean
    private IsLockedServices isLockedServices;
    @MockBean
    private NbaAPIClient nbaAPIClient;
    @MockBean
    private DiscordPlayerGuildServices discordPlayerGuildServices;
    @MockBean
    private JwtService jwtService;

    private static final LocalDate TEST_DATE = LocalDate.parse("2025-12-25");
    private static final String AUTHENTICATED_USER = "test-user-id";

    @BeforeEach
    void setUp() {
        // Configure JwtService mock to validate test tokens
        given(jwtService.isTokenValid(TEST_TOKEN)).willReturn(true);
        Claims claims = new DefaultClaims(Map.of("id", "test-user-id", "username", "test-user"));
        given(jwtService.parseToken(TEST_TOKEN)).willReturn(claims);
    }

    @Test
    void todaysPlayers_returnsJoinedRows() throws Exception {
        NbaPlayerTeam row = new NbaPlayerTeam();
        row.setPlayer_name("LeBron James");
        row.setTeam_name("LAL");
        row.setAgainst_team_name("BOS");
        row.setDate("2025-12-25");
        given(nbaPlayerServices.getNbaPlayersWithTeam(TEST_DATE)).willReturn(List.of(row));

        mockMvc.perform(get("/api/activity/todays-players")
                        .header("Authorization", AUTH_HEADER)
                        .param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].player_name", is("LeBron James")))
                .andExpect(jsonPath("$[0].team_name", is("LAL")));
    }

    @Test
    void liveData_returnsString() throws Exception {
        given(nbaAPIClient.getGamesData("123")).willReturn("OK");
        mockMvc.perform(get("/api/activity/livedata/123")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void myRoster_insertsGuildAndChannel_thenReturnsRoster() throws Exception {
        DailyRosterPlayer p = DailyRosterPlayer.builder()
                .name("LeBron James").dollarValue(12000).build();
        given(dailyRosterServices.getPlayerRoster("player-1", TEST_DATE)).willReturn(List.of(p));

        mockMvc.perform(get("/api/activity/my-roster/{guildId}/{channelId}/{discordPlayerId}", "guild-1", "chan-1", "player-1")
                        .header("Authorization", AUTH_HEADER)
                        .param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("LeBron James")));

        // verify side effects
        verify(discordPlayerGuildServices).insertGuildForPlayerId("player-1", "guild-1");
        verify(discordPlayerGuildServices).insertChannelForDate("chan-1", "guild-1");
    }

    @Test
    void setPlayer_returnsBadRequest_ifPastLockTime() throws Exception {
        given(dailyRosterServices.saveRosterChoiceWithinCap(any(), any(), any(), any(), any(), eq(ActivitiesController.MAX_DOLLARS)))
                .willReturn(RosterWriteOutcome.of(Status.LOCKED));

        mockMvc.perform(post("/api/activity/my-roster")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rosterPayload(AUTHENTICATED_USER)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("past the lock time")));
    }

    @Test
    void setPlayer_returnsNotFound_whenNoGamesAreScheduled() throws Exception {
        given(dailyRosterServices.saveRosterChoiceWithinCap(any(), any(), any(), any(), any(), eq(ActivitiesController.MAX_DOLLARS)))
                .willReturn(RosterWriteOutcome.of(Status.NO_GAMES));

        mockMvc.perform(post("/api/activity/my-roster")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rosterPayload(AUTHENTICATED_USER)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No games scheduled")));
    }

    @Test
    void setPlayer_returnsForbidden_whenPlayerIdDoesNotMatchJwt() throws Exception {
        mockMvc.perform(post("/api/activity/my-roster")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rosterPayload("someone-else")))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Forbidden")));

        verify(dailyRosterServices, never()).saveRosterChoiceWithinCap(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void setPlayer_success_whenAffordable_andBeforeLock() throws Exception {
        given(dailyRosterServices.saveRosterChoiceWithinCap(any(), eq(AUTHENTICATED_USER), any(), any(), any(), eq(ActivitiesController.MAX_DOLLARS)))
                .willReturn(RosterWriteOutcome.of(Status.SAVED));

        mockMvc.perform(post("/api/activity/my-roster")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rosterPayload(AUTHENTICATED_USER)))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void leaderboards_and_lockTime_endpoints() throws Exception {
        DailyRosterPlayer p = DailyRosterPlayer.builder().name("LeBron James").build();
        given(dailyRosterServices.getGlobalLeaderboard(TEST_DATE)).willReturn(List.of(p));
        given(dailyRosterServices.getWeeklyGuildLeaderboard("g1", TEST_DATE)).willReturn(List.of(p));
        given(dailyRosterServices.getGuildLeaderboard("g1", TEST_DATE)).willReturn(List.of(p));
        given(isLockedServices.findLock(TEST_DATE)).willReturn(Optional.of(
                IsLocked.builder().date(TEST_DATE).lockTime(OffsetDateTime.now().plusHours(1)).build()));

        mockMvc.perform(get("/api/activity/rosters/global")
                        .header("Authorization", AUTH_HEADER)
                        .param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("LeBron James")));

        mockMvc.perform(get("/api/activity/rosters/weekly/{guildId}", "g1")
                        .header("Authorization", AUTH_HEADER)
                        .param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("LeBron James")));

        mockMvc.perform(get("/api/activity/rosters/{guildId}", "g1")
                        .header("Authorization", AUTH_HEADER)
                        .param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("LeBron James")));

        mockMvc.perform(get("/api/activity/lock-time")
                        .header("Authorization", AUTH_HEADER)
                        .param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date", is("2025-12-25")));
    }

    @Test
    void lockTime_returnsNotFound_whenNoGamesAreScheduled() throws Exception {
        given(isLockedServices.findLock(TEST_DATE)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/activity/lock-time")
                        .header("Authorization", AUTH_HEADER)
                        .param("date", "2025-12-25"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No games scheduled")));
    }

    @Test
    void deleteRosterPlayer_callsService_andReturnsOk() throws Exception {
        given(dailyRosterServices.deleteRosterPlayerIfOpen(any())).willReturn(RosterWriteOutcome.of(Status.DELETED));

        mockMvc.perform(delete("/api/activity/my-roster")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deletePayload(AUTHENTICATED_USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("deleted successfully")));
    }

    @Test
    void deleteRosterPlayer_returnsNotFound_whenNoGamesAreScheduled() throws Exception {
        given(dailyRosterServices.deleteRosterPlayerIfOpen(any())).willReturn(RosterWriteOutcome.of(Status.NO_GAMES));

        mockMvc.perform(delete("/api/activity/my-roster")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deletePayload(AUTHENTICATED_USER)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No games scheduled")));
    }

    @Test
    void deleteRosterPlayer_returnsBadRequest_whenLocked() throws Exception {
        given(dailyRosterServices.deleteRosterPlayerIfOpen(any())).willReturn(RosterWriteOutcome.of(Status.LOCKED));

        mockMvc.perform(delete("/api/activity/my-roster")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deletePayload(AUTHENTICATED_USER)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("past the lock time")));
    }

    @Test
    void deleteRosterPlayer_returnsForbidden_whenPlayerIdDoesNotMatchJwt() throws Exception {
        mockMvc.perform(delete("/api/activity/my-roster")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deletePayload("someone-else")))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Forbidden")));

        verify(dailyRosterServices, never()).deleteRosterPlayerIfOpen(any());
    }

    private static String rosterPayload(String discordPlayerId) {
        return """
                {
                  "nba_player_uid": "11111111-1111-1111-1111-111111111111",
                  "discord_player_id": "%s",
                  "nickname": "King",
                  "position": "SF"
                }
                """.formatted(discordPlayerId);
    }

    private static String deletePayload(String discordPlayerId) {
        return """
                {
                  "nbaPlayerUid": "11111111-1111-1111-1111-111111111111",
                  "discordPlayerId": "%s",
                  "date": "2025-12-25"
                }
                """.formatted(discordPlayerId);
    }
}
