package com.picknroll.demo.services;

import com.picknroll.demo.gacha.GachaEconomy;
import com.picknroll.demo.gacha.GachaRarity;
import com.picknroll.demo.models.dtos.GachaSkin;
import com.picknroll.demo.models.dtos.GachaUserWallet;
import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.models.gacha.GachaPullResponse;
import com.picknroll.demo.models.gacha.WeightedPlayer;
import com.picknroll.demo.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GachaServicesTest {

    @Mock GachaUserWalletRepository walletRepository;
    @Mock GachaSkinRepository skinRepository;
    @Mock GachaUserSkinRepository userSkinRepository;
    @Mock GachaTokenGrantRepository grantRepository;
    @Mock DiscordPlayerGuildRepository discordPlayerGuildRepository;
    @Mock DailyRosterRepository dailyRosterRepository;
    @Mock IsLockedRepository isLockedRepository;
    @Mock IsLockedServices isLockedServices;
    @Mock GachaPoolService gachaPoolService;
    @Mock JdbcTemplate jdbcTemplate;

    GachaServices services;

    @BeforeEach
    void setUp() {
        services = new GachaServices(
                walletRepository,
                skinRepository,
                userSkinRepository,
                grantRepository,
                discordPlayerGuildRepository,
                dailyRosterRepository,
                isLockedRepository,
                isLockedServices,
                gachaPoolService,
                jdbcTemplate);
    }

    @Test
    @DisplayName("pull mutation rejects invalid count without debiting")
    void mutationsRejectInvalidCount() {
        GachaServices.PullOutcome outcome = services.pull("u1", 5);
        assertFalse(outcome.success());
        assertEquals(GachaServices.INVALID_COUNT, outcome.error());
        verify(walletRepository, never()).tryDebitRolls(any(), anyInt());
        verify(userSkinRepository, never()).insertOwnership(any(), any());
    }

    @Test
    @DisplayName("pull mutation rejects insufficient Rolls without writing inventory")
    void mutationsRejectInsufficientRolls() {
        given(jdbcTemplate.execute(anyString(), any(PreparedStatementCallback.class))).willReturn(null);
        given(walletRepository.tryDebitRolls("u1", 100)).willReturn(0);

        GachaServices.PullOutcome outcome = services.pull("u1", 1);
        assertFalse(outcome.success());
        assertEquals(GachaServices.INSUFFICIENT_ROLLS, outcome.error());
        verify(userSkinRepository, never()).insertOwnership(any(), any());
        verify(walletRepository, never()).addDust(any(), anyInt());
        verify(grantRepository, never()).tryInsertGrant(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("pull mutation rejects empty pool and refunds the debit")
    void mutationsRejectEmptyPoolAndRefund() {
        given(jdbcTemplate.execute(anyString(), any(PreparedStatementCallback.class))).willReturn(null);
        given(walletRepository.tryDebitRolls("u1", 100)).willReturn(1);
        given(gachaPoolService.loadWeightedPool()).willReturn(List.of());

        GachaServices.PullOutcome outcome = services.pull("u1", 1);
        assertFalse(outcome.success());
        assertEquals(GachaServices.EMPTY_POOL, outcome.error());
        verify(walletRepository).addRolls("u1", 100);
        verify(userSkinRepository, never()).insertOwnership(any(), any());
    }

    @Test
    @DisplayName("10-pull mutation rejects when wallet cannot cover full batch cost")
    void mutationsRejectTenPullWhenShortOnRolls() {
        given(jdbcTemplate.execute(anyString(), any(PreparedStatementCallback.class))).willReturn(null);
        given(walletRepository.tryDebitRolls("u1", 1000)).willReturn(0);

        GachaServices.PullOutcome outcome = services.pull("u1", 10);
        assertFalse(outcome.success());
        assertEquals(GachaServices.INSUFFICIENT_ROLLS, outcome.error());
        verify(gachaPoolService, never()).loadWeightedPool();
    }

    @Test
    void pullNewSkinInsertsOwnership() {
        UUID skinId = UUID.randomUUID();
        stubOpenPull(skinId, false);

        GachaServices.PullOutcome outcome = services.pull("u1", 1);
        assertTrue(outcome.success());
        GachaPullResponse response = outcome.response();
        assertEquals(1, response.getResults().size());
        assertEquals("NEW", response.getResults().get(0).getOutcome());
        verify(userSkinRepository).insertOwnership("u1", skinId);
        verify(walletRepository, never()).addDust(eq("u1"), anyInt());
    }

    @Test
    void pullDuplicateGrantsDust() {
        UUID skinId = UUID.randomUUID();
        stubOpenPull(skinId, true);

        GachaServices.PullOutcome outcome = services.pull("u1", 1);
        assertTrue(outcome.success());
        assertEquals("DUPLICATE", outcome.response().getResults().get(0).getOutcome());
        verify(walletRepository).addDust("u1", GachaEconomy.dustFor(
                GachaRarity.fromString(outcome.response().getResults().get(0).getRarity())));
        verify(userSkinRepository, never()).insertOwnership(any(), any());
    }

    @Test
    void hardPityForcesLegendaryRateTable() {
        Map<GachaRarity, Double> rates = GachaEconomy.ratesForPity(80);
        assertEquals(GachaRarity.LEGENDARY, services.rollRarity(rates));
    }

    @Test
    void weightedDrawPrefersHeavierPlayersOverManyTrials() {
        List<WeightedPlayer> pool = List.of(
                new WeightedPlayer(1L, 10, 10),
                new WeightedPlayer(2L, 90, 90));
        int heavy = 0;
        for (int i = 0; i < 2000; i++) {
            if (services.weightedDraw(pool).nbaPlayerId() == 2L) {
                heavy++;
            }
        }
        assertTrue(heavy > 1500, "expected heavy player ~90%, got " + heavy);
    }

    @Test
    void rosterCompleteGrantIsIdempotent() {
        given(grantRepository.tryInsertGrant("u1", GachaEconomy.REASON_ROSTER_COMPLETE, "2025-12-25", 40))
                .willReturn(0);
        assertFalse(services.tryRosterCompleteGrant("u1", LocalDate.parse("2025-12-25")));
        verify(walletRepository, never()).addRolls(any(), anyInt());
    }

    @Test
    void rosterCompleteGrantCreditsAndUpdatesStreak() {
        LocalDate day = LocalDate.parse("2025-12-25");
        given(grantRepository.tryInsertGrant("u1", GachaEconomy.REASON_ROSTER_COMPLETE, day.toString(), 40))
                .willReturn(1);
        given(walletRepository.findWallet("u1")).willReturn(Optional.of(GachaUserWallet.builder()
                .discordPlayerId("u1")
                .streakDays(2)
                .streakLastDate(LocalDate.parse("2025-12-24"))
                .build()));
        given(isLockedRepository.findPreviousGameDay(day)).willReturn(Optional.of(
                IsLocked.builder().date(LocalDate.parse("2025-12-24")).lockTime(OffsetDateTime.now()).build()));
        given(grantRepository.tryInsertGrant("u1", GachaEconomy.REASON_STREAK_3, day.toString(), 20))
                .willReturn(1);

        assertTrue(services.tryRosterCompleteGrant("u1", day));
        verify(walletRepository).addRolls("u1", 40);
        verify(walletRepository).updateStreak("u1", 3, day);
        verify(walletRepository).addRolls("u1", 20);
    }

    private void stubOpenPull(UUID skinId, boolean alreadyOwned) {
        given(jdbcTemplate.execute(anyString(), any(PreparedStatementCallback.class))).willReturn(null);
        given(walletRepository.tryDebitRolls("u1", 100)).willReturn(1);
        given(gachaPoolService.loadWeightedPool()).willReturn(List.of(new WeightedPlayer(1L, 30, 30)));
        given(walletRepository.findWallet("u1")).willReturn(Optional.of(GachaUserWallet.builder()
                .discordPlayerId("u1")
                .rolls(900)
                .dust(0)
                .pityCounter(0)
                .build()));
        // Stub all rarities — which one is rolled is random.
        for (GachaRarity rarity : GachaRarity.values()) {
            lenient().when(skinRepository.findByNbaPlayerIdAndRarity(1L, rarity.name())).thenReturn(Optional.of(
                    GachaSkin.builder()
                            .skinId(skinId)
                            .nbaPlayerId(1L)
                            .rarity(rarity.name())
                            .artUrl("art")
                            .frameKey("frame_" + rarity.name().toLowerCase())
                            .build()));
        }
        given(userSkinRepository.countOwnership("u1", skinId)).willReturn(alreadyOwned ? 1 : 0);
        if (!alreadyOwned) {
            lenient().when(userSkinRepository.insertOwnership("u1", skinId)).thenReturn(1);
        }
    }
}
