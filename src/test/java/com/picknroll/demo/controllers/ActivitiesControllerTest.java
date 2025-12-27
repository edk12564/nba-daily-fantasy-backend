package com.picknroll.demo.controllers;

import com.picknroll.demo.httpclient.NbaAPIClient;
import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.models.joinTables.NbaPlayerTeam;
import com.picknroll.demo.services.DailyRosterServices;
import com.picknroll.demo.services.DiscordPlayerGuildServices;
import com.picknroll.demo.services.IsLockedServices;
import com.picknroll.demo.services.NbaPlayerServices;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
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
        "discord.client.secret=test-secret"
})
class ActivitiesControllerTest {

    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        JdbcRepositoriesAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
    })
    @ComponentScan(
        basePackages = "com.picknroll.demo.controllers",
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

    private static final LocalDate TEST_DATE = LocalDate.parse("2025-12-25");

    @Test
    void todaysPlayers_returnsJoinedRows() throws Exception {
        NbaPlayerTeam row = new NbaPlayerTeam();
        row.setPlayer_name("LeBron James");
        row.setTeam_name("LAL");
        row.setAgainst_team_name("BOS");
        row.setDate("2025-12-25");
        given(nbaPlayerServices.getNbaPlayersWithTeam(TEST_DATE)).willReturn(List.of(row));

        mockMvc.perform(get("/api/activity/todays-players").param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].player_name", is("LeBron James")))
                .andExpect(jsonPath("$[0].team_name", is("LAL")));
    }

    @Test
    void liveData_returnsString() throws Exception {
        given(nbaAPIClient.getGamesData("123")).willReturn("OK");
        mockMvc.perform(get("/api/activity/livedata/123"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void myRoster_insertsGuildAndChannel_thenReturnsRoster() throws Exception {
        DailyRosterPlayer p = DailyRosterPlayer.builder()
                .name("LeBron James").dollarValue(12000).build();
        given(dailyRosterServices.getPlayerRoster("player-1", TEST_DATE)).willReturn(List.of(p));

        mockMvc.perform(get("/api/activity/my-roster/{guildId}/{channelId}/{discordPlayerId}", "guild-1", "chan-1", "player-1")
                        .param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("LeBron James")));

        // verify side effects
        verify(discordPlayerGuildServices).insertGuildForPlayerId("player-1", "guild-1");
        verify(discordPlayerGuildServices).insertChannelForDate("chan-1", "guild-1");
    }

    @Test
    void setPlayer_returnsBadRequest_ifPastLockTime() throws Exception {
        // lock time before now
        IsLocked past = IsLocked.builder().date(TEST_DATE).lockTime(OffsetDateTime.now().minusMinutes(5)).build();
        given(isLockedServices.isLocked(any(LocalDate.class))).willReturn(past);

        String payload = """
                {
                  "nba_player_uid": "11111111-1111-1111-1111-111111111111",
                  "discord_player_id": "p1",
                  "nickname": "King",
                  "position": "SF"
                }
                """;

        mockMvc.perform(post("/api/activity/my-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("past the lock time")));
    }

    @Test
    void setPlayer_success_whenAffordable_andBeforeLock() throws Exception {
        IsLocked future = IsLocked.builder().date(TEST_DATE).lockTime(OffsetDateTime.now().plusMinutes(10)).build();
        given(isLockedServices.isLocked(any(LocalDate.class))).willReturn(future);
        given(dailyRosterServices.getTodaysRosterPriceWithPlayer(anyString(), anyString(), any(LocalDate.class), any(UUID.class)))
                .willReturn(100);
        given(dailyRosterServices.saveRosterChoice(any(UUID.class), anyString(), anyString(), anyString(), any(LocalDate.class)))
                .willReturn(1);

        String payload = """
                {
                  "nba_player_uid": "11111111-1111-1111-1111-111111111111",
                  "discord_player_id": "p1",
                  "nickname": "King",
                  "position": "SF"
                }
                """;

        mockMvc.perform(post("/api/activity/my-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void leaderboards_and_lockTime_endpoints() throws Exception {
        DailyRosterPlayer p = DailyRosterPlayer.builder().name("LeBron James").build();
        given(dailyRosterServices.getGlobalLeaderboard(TEST_DATE)).willReturn(List.of(p));
        given(dailyRosterServices.getWeeklyGuildLeaderboard("g1", TEST_DATE)).willReturn(List.of(p));
        given(dailyRosterServices.getGuildLeaderboard("g1", TEST_DATE)).willReturn(List.of(p));
        given(isLockedServices.isLocked(TEST_DATE)).willReturn(IsLocked.builder().date(TEST_DATE).lockTime(OffsetDateTime.now().plusHours(1)).build());

        mockMvc.perform(get("/api/activity/rosters/global").param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("LeBron James")));

        mockMvc.perform(get("/api/activity/rosters/weekly/{guildId}", "g1").param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("LeBron James")));

        mockMvc.perform(get("/api/activity/rosters/{guildId}", "g1").param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("LeBron James")));

        mockMvc.perform(get("/api/activity/lock-time").param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date", is("2025-12-25")));
    }

    @Test
    void deleteRosterPlayer_callsService_andReturnsOk() throws Exception {
        String payload = """
                {
                  "nbaPlayerUid": "11111111-1111-1111-1111-111111111111",
                  "discordPlayerId": "p1",
                  "date": "2025-12-25"
                }
                """;

        mockMvc.perform(delete("/api/activity/my-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("deleted successfully")));
    }
}
