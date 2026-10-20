package com.picknroll.demo.controllers;

import com.picknroll.demo.models.gacha.GachaInventoryItem;
import com.picknroll.demo.models.gacha.GachaPullResponse;
import com.picknroll.demo.models.gacha.GachaRatesResponse;
import com.picknroll.demo.models.gacha.GachaWalletResponse;
import com.picknroll.demo.services.GachaServices;
import com.picknroll.demo.services.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = GachaControllerTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "discord.client.id=test-client-id",
        "discord.client.secret=test-secret",
        "jwt.secret=test-secret-key-for-jwt-signing-minimum-32-characters"
})
class GachaControllerTest {

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final String AUTH_HEADER = "Bearer " + TEST_TOKEN;
    private static final String USER = "gacha-user";

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
    private GachaServices gachaServices;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private com.picknroll.demo.services.NbaPlayerServices nbaPlayerServices;
    @MockBean
    private com.picknroll.demo.services.DailyRosterServices dailyRosterServices;
    @MockBean
    private com.picknroll.demo.services.IsLockedServices isLockedServices;
    @MockBean
    private com.picknroll.demo.httpclient.NbaAPIClient nbaAPIClient;
    @MockBean
    private com.picknroll.demo.services.NbaScheduleService nbaScheduleService;
    @MockBean
    private com.picknroll.demo.services.DiscordPlayerGuildServices discordPlayerGuildServices;

    @BeforeEach
    void setUp() {
        given(jwtService.isTokenValid(TEST_TOKEN)).willReturn(true);
        Claims claims = new DefaultClaims(Map.of("id", USER, "username", "gacha"));
        given(jwtService.parseToken(TEST_TOKEN)).willReturn(claims);
    }

    @Test
    void walletReturnsBalance() throws Exception {
        given(gachaServices.getWallet(USER)).willReturn(GachaWalletResponse.builder()
                .rolls(140).dust(5).pityCounter(3).streakDays(2).build());

        mockMvc.perform(get("/api/activity/gacha/wallet").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolls").value(140))
                .andExpect(jsonPath("$.dust").value(5));
    }

    @Test
    void ratesArePublicUnderJwt() throws Exception {
        given(gachaServices.getRates()).willReturn(GachaRatesResponse.builder()
                .pullCost(100)
                .rarityRates(Map.of("COMMON", 0.7))
                .dustOnDuplicate(Map.of("COMMON", 5))
                .softPityStart(40)
                .hardPityAt(80)
                .weightFloor(8)
                .build());

        mockMvc.perform(get("/api/activity/gacha/rates").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pullCost").value(100));
    }

    @Test
    void pullMutation_returnsBadRequest_whenInsufficientRolls() throws Exception {
        given(gachaServices.pull(eq(USER), eq(1)))
                .willReturn(GachaServices.PullOutcome.error(GachaServices.INSUFFICIENT_ROLLS));

        mockMvc.perform(post("/api/activity/gacha/pull")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Insufficient Rolls")));
    }

    @Test
    void pullMutation_returnsBadRequest_whenCountInvalid() throws Exception {
        given(gachaServices.pull(eq(USER), eq(3)))
                .willReturn(GachaServices.PullOutcome.error(GachaServices.INVALID_COUNT));

        mockMvc.perform(post("/api/activity/gacha/pull")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("count must be 1 or 10")));
    }

    @Test
    void pullMutation_returnsBadRequest_whenPoolEmpty() throws Exception {
        given(gachaServices.pull(eq(USER), eq(1)))
                .willReturn(GachaServices.PullOutcome.error(GachaServices.EMPTY_POOL));

        mockMvc.perform(post("/api/activity/gacha/pull")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("No eligible players")));
    }

    @Test
    void pullMutation_returnsForbidden_withoutJwtUser() throws Exception {
        given(jwtService.isTokenValid(TEST_TOKEN)).willReturn(true);
        Claims claims = new DefaultClaims(Map.of("username", "no-id"));
        given(jwtService.parseToken(TEST_TOKEN)).willReturn(claims);

        mockMvc.perform(post("/api/activity/gacha/pull")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Forbidden")));

        verify(gachaServices, never()).pull(anyString(), anyInt());
    }

    @Test
    void pullMutation_success_returnsPayload() throws Exception {
        given(gachaServices.pull(eq(USER), eq(1))).willReturn(GachaServices.PullOutcome.ok(
                GachaPullResponse.builder().results(List.of()).rolls(0).dust(0).pityCounter(1).build()));

        mockMvc.perform(post("/api/activity/gacha/pull")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pityCounter").value(1));
    }

    @Test
    void walletMutation_returnsForbidden_withoutJwtUser() throws Exception {
        Claims claims = new DefaultClaims(Map.of("username", "no-id"));
        given(jwtService.parseToken(TEST_TOKEN)).willReturn(claims);

        mockMvc.perform(get("/api/activity/gacha/wallet").header("Authorization", AUTH_HEADER))
                .andExpect(status().isForbidden());
        verify(gachaServices, never()).getWallet(anyString());
    }

    @Test
    void inventoryReturnsOwnedSkins() throws Exception {
        given(gachaServices.getInventory(USER)).willReturn(List.of(
                GachaInventoryItem.builder()
                        .nbaPlayerId(1L)
                        .rarity("EPIC")
                        .displaySkin(true)
                        .frameKey("frame_epic")
                        .build()));

        mockMvc.perform(get("/api/activity/gacha/inventory").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rarity").value("EPIC"))
                .andExpect(jsonPath("$[0].displaySkin").value(true));
    }

    @Test
    void inventoryMutation_returnsForbidden_withoutJwtUser() throws Exception {
        Claims claims = new DefaultClaims(Map.of("username", "no-id"));
        given(jwtService.parseToken(TEST_TOKEN)).willReturn(claims);

        mockMvc.perform(get("/api/activity/gacha/inventory").header("Authorization", AUTH_HEADER))
                .andExpect(status().isForbidden());
        verify(gachaServices, never()).getInventory(anyString());
    }

    @Test
    void pullMutation_zeroCountTreatedAsInvalidCount() throws Exception {
        given(gachaServices.pull(eq(USER), eq(0)))
                .willReturn(GachaServices.PullOutcome.error(GachaServices.INVALID_COUNT));

        mockMvc.perform(post("/api/activity/gacha/pull")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("count must be 1 or 10")));
    }

    @Test
    void pullMutation_unknownErrorMapsToGenericBadRequest() throws Exception {
        given(gachaServices.pull(eq(USER), eq(1)))
                .willReturn(GachaServices.PullOutcome.error("SOMETHING_ELSE"));

        mockMvc.perform(post("/api/activity/gacha/pull")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Pull failed")));
    }
}
