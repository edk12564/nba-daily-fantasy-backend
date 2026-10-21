package com.picknroll.demo.services;

import com.picknroll.demo.gacha.GachaEconomy;
import com.picknroll.demo.gacha.GachaRarity;
import com.picknroll.demo.models.dtos.GachaSkin;
import com.picknroll.demo.models.dtos.GachaUserWallet;
import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.models.gacha.GachaInventoryItem;
import com.picknroll.demo.models.gacha.GachaPullResponse;
import com.picknroll.demo.models.gacha.GachaRatesResponse;
import com.picknroll.demo.models.gacha.GachaWalletResponse;
import com.picknroll.demo.models.gacha.WeightedPlayer;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.repositories.*;
import com.picknroll.demo.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
        // Default: starter already claimed (no double-credit in grant/pull tests)
        lenient()
                .when(grantRepository.tryInsertGrant(
                        anyString(),
                        eq(GachaEconomy.REASON_STARTER),
                        eq(GachaEconomy.PERIOD_STARTER),
                        eq(GachaEconomy.STARTER_ROLLS)))
                .thenReturn(0);
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

        given(grantRepository.tryInsertGrant(
                        "u1",
                        GachaEconomy.REASON_STARTER,
                        GachaEconomy.PERIOD_STARTER,
                        GachaEconomy.STARTER_ROLLS))
                .willReturn(0);

        GachaServices.PullOutcome outcome = services.pull("u1", 1);
        assertFalse(outcome.success());
        assertEquals(GachaServices.INSUFFICIENT_ROLLS, outcome.error());
        verify(userSkinRepository, never()).insertOwnership(any(), any());
        verify(walletRepository, never()).addDust(any(), anyInt());
        verify(walletRepository, never()).addRolls(any(), anyInt());
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
                new WeightedPlayer(1L, 10, 10, "A"),
                new WeightedPlayer(2L, 90, 90, "B"));
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
        given(grantRepository.tryInsertGrant(
                        "u1",
                        GachaEconomy.REASON_STARTER,
                        GachaEconomy.PERIOD_STARTER,
                        GachaEconomy.STARTER_ROLLS))
                .willReturn(0);
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

    @Test
    @DisplayName("streak hits 7 and 14 grant the matching bonuses")
    void streakSevenAndFourteenGrantBonuses() {
        assertStreakBonus(6, 7, GachaEconomy.REASON_STREAK_7, GachaEconomy.STREAK_7_ROLLS);
        assertStreakBonus(13, 14, GachaEconomy.REASON_STREAK_14, GachaEconomy.STREAK_14_ROLLS);
    }

    @Test
    @DisplayName("streak resets when last complete was not the previous game day")
    void streakResetsWhenPreviousGameDayDoesNotMatch() {
        LocalDate day = LocalDate.parse("2025-12-25");
        given(grantRepository.tryInsertGrant("u1", GachaEconomy.REASON_ROSTER_COMPLETE, day.toString(), 40))
                .willReturn(1);
        given(walletRepository.findWallet("u1")).willReturn(Optional.of(GachaUserWallet.builder()
                .discordPlayerId("u1")
                .streakDays(5)
                .streakLastDate(LocalDate.parse("2025-12-20"))
                .build()));
        given(isLockedRepository.findPreviousGameDay(day)).willReturn(Optional.of(
                IsLocked.builder().date(LocalDate.parse("2025-12-24")).lockTime(OffsetDateTime.now()).build()));

        assertTrue(services.tryRosterCompleteGrant("u1", day));
        verify(walletRepository).updateStreak("u1", 1, day);
        verify(grantRepository, never()).tryInsertGrant(eq("u1"), eq(GachaEconomy.REASON_STREAK_3), any(), anyInt());
    }

    @Test
    @DisplayName("legendary pull resets pity counter to zero")
    void legendaryPullResetsPity() {
        UUID skinId = UUID.randomUUID();
        stubOpenPull(skinId, false);
        given(walletRepository.findWallet("u1")).willReturn(Optional.of(GachaUserWallet.builder()
                .discordPlayerId("u1")
                .rolls(900)
                .dust(0)
                .pityCounter(40)
                .build()));

        GachaServices spy = spy(services);
        doReturn(GachaRarity.LEGENDARY).when(spy).rollRarity(any());

        GachaServices.PullOutcome outcome = spy.pull("u1", 1);
        assertTrue(outcome.success());
        assertEquals("LEGENDARY", outcome.response().getResults().get(0).getRarity());
        verify(walletRepository).setPityCounter("u1", 0);
    }

    @Test
    @DisplayName("10-pull with all Commons upgrades one result to Rare+")
    void tenPullGuaranteesRarePlus() {
        UUID commonId = UUID.randomUUID();
        UUID rareId = UUID.randomUUID();
        given(jdbcTemplate.execute(anyString(), any(PreparedStatementCallback.class))).willReturn(null);
        given(walletRepository.tryDebitRolls("u1", 1000)).willReturn(1);
        given(gachaPoolService.loadWeightedPool()).willReturn(List.of(new WeightedPlayer(1L, 30, 30, "Test Player")));
        given(walletRepository.findWallet("u1")).willReturn(Optional.of(GachaUserWallet.builder()
                .discordPlayerId("u1")
                .rolls(0)
                .dust(0)
                .pityCounter(0)
                .build()));
        for (GachaRarity rarity : GachaRarity.values()) {
            UUID id = rarity == GachaRarity.COMMON ? commonId : rareId;
            lenient().when(skinRepository.findByNbaPlayerIdAndRarity(1L, rarity.name())).thenReturn(Optional.of(
                    GachaSkin.builder()
                            .skinId(id)
                            .nbaPlayerId(1L)
                            .rarity(rarity.name())
                            .artUrl("art")
                            .frameKey("frame")
                            .build()));
        }
        given(userSkinRepository.countOwnership(eq("u1"), any())).willReturn(0);
        lenient().when(userSkinRepository.insertOwnership(eq("u1"), any())).thenReturn(1);

        GachaServices spy = spy(services);
        doAnswer(invocation -> {
            Map<GachaRarity, Double> rates = invocation.getArgument(0);
            if (rates.getOrDefault(GachaRarity.COMMON, 1.0) == 0.0) {
                return GachaRarity.RARE;
            }
            return GachaRarity.COMMON;
        }).when(spy).rollRarity(any());

        GachaServices.PullOutcome outcome = spy.pull("u1", 10);
        assertTrue(outcome.success());
        assertEquals(10, outcome.response().getResults().size());
        assertTrue(outcome.response().getResults().stream()
                .anyMatch(r -> GachaRarity.fromString(r.getRarity()).isRareOrHigher()));
        verify(userSkinRepository).deleteOwnership("u1", commonId);
    }

    @Test
    void inventoryMarksOnlyMaxRarityAsDisplaySkin() {
        UUID common = UUID.randomUUID();
        UUID epic = UUID.randomUUID();
        given(skinRepository.findOwnedSkins("u1")).willReturn(List.of(
                GachaSkin.builder().skinId(common).nbaPlayerId(1L).rarity("COMMON")
                        .artUrl("a").frameKey("frame_common").build(),
                GachaSkin.builder().skinId(epic).nbaPlayerId(1L).rarity("EPIC")
                        .artUrl("a").frameKey("frame_epic").build()));
        given(gachaPoolService.loadWeightedPool()).willReturn(List.of(new WeightedPlayer(1L, 30, 30, "Test Player")));

        List<GachaInventoryItem> items = services.getInventory("u1");
        assertEquals(2, items.size());
        GachaInventoryItem commonItem = items.stream().filter(i -> "COMMON".equals(i.getRarity())).findFirst().orElseThrow();
        GachaInventoryItem epicItem = items.stream().filter(i -> "EPIC".equals(i.getRarity())).findFirst().orElseThrow();
        assertFalse(commonItem.isDisplaySkin());
        assertTrue(epicItem.isDisplaySkin());
        assertEquals(5, epicItem.getPlayerStar()); // dollar_value 30 → ★5 (≥29)
    }

    @Test
    void getRatesPublishesTables() {
        GachaRatesResponse rates = services.getRates();
        assertEquals(100, rates.getPullCost());
        assertEquals(0.70, rates.getRarityRates().get("COMMON"), 1e-9);
        assertEquals(5, rates.getDustOnDuplicate().get("COMMON"));
        assertEquals(40, rates.getSoftPityStart());
        assertEquals(80, rates.getHardPityAt());
        assertEquals(0, rates.getSalaryDiscountByRarity().get("COMMON"));
        assertEquals(1, rates.getSalaryDiscountByRarity().get("RARE"));
        assertEquals(2, rates.getSalaryDiscountByRarity().get("EPIC"));
        assertEquals(3, rates.getSalaryDiscountByRarity().get("LEGENDARY"));
    }

    @Test
    void getWalletReturnsBalanceAfterEnsure() {
        given(walletRepository.findWallet("u1")).willReturn(Optional.of(GachaUserWallet.builder()
                .discordPlayerId("u1")
                .rolls(140)
                .dust(9)
                .pityCounter(2)
                .streakDays(1)
                .build()));
        given(discordPlayerGuildRepository.findGuildIdsByDiscordPlayerId("u1")).willReturn(List.of());

        given(grantRepository.tryInsertGrant(
                        "u1",
                        GachaEconomy.REASON_STARTER,
                        GachaEconomy.PERIOD_STARTER,
                        GachaEconomy.STARTER_ROLLS))
                .willReturn(0);

        GachaWalletResponse wallet = services.getWallet("u1");
        assertEquals(140, wallet.getRolls());
        assertEquals(9, wallet.getDust());
        assertEquals(2, wallet.getPityCounter());
        verify(walletRepository).ensureWallet("u1");
        verify(grantRepository).tryInsertGrant(
                "u1",
                GachaEconomy.REASON_STARTER,
                GachaEconomy.PERIOD_STARTER,
                GachaEconomy.STARTER_ROLLS);
        verify(walletRepository, never()).addRolls("u1", GachaEconomy.STARTER_ROLLS);
    }

    @Test
    @DisplayName("podium mutation skips grants when scores are not settled")
    void mutationsRejectPodiumWhenNotSettled() {
        LocalDate yesterday = Utils.getCaliforniaDate().minusDays(1);
        given(discordPlayerGuildRepository.findGuildIdsByDiscordPlayerId("u1")).willReturn(List.of("guild-1"));
        given(isLockedServices.findLock(any())).willReturn(Optional.empty());

        services.claimPendingPodiumGrants("u1");

        verify(grantRepository, never()).tryInsertGrant(any(), any(), any(), anyInt());
        verify(dailyRosterRepository, never()).getTodaysLeaderboardByGuildId(any(), eq(yesterday));
    }

    @Test
    @DisplayName("daily podium grants 1st place Rolls once scores settle")
    void dailyPodiumGrantsFirstPlace() {
        LocalDate yesterday = Utils.getCaliforniaDate().minusDays(1);
        given(discordPlayerGuildRepository.findGuildIdsByDiscordPlayerId("u1")).willReturn(List.of("guild-1"));
        given(isLockedServices.findLock(yesterday)).willReturn(Optional.of(
                IsLocked.builder().date(yesterday).lockTime(OffsetDateTime.now().minusHours(1)).build()));
        // weekly path also checks weekEnd lock — leave unset so weekly is skipped
        given(isLockedServices.findLock(argThat(d -> d != null && !d.equals(yesterday))))
                .willReturn(Optional.empty());
        given(dailyRosterRepository.getTodaysLeaderboardByGuildId("guild-1", yesterday)).willReturn(List.of(
                DailyRosterPlayer.builder().discordPlayerId("u1").fantasyScore(50.0).build(),
                DailyRosterPlayer.builder().discordPlayerId("u2").fantasyScore(10.0).build()));
        given(grantRepository.tryInsertGrant(
                "u1",
                GachaEconomy.REASON_GUILD_DAILY_PODIUM,
                "guild-1:" + yesterday,
                GachaEconomy.GUILD_DAILY_1ST)).willReturn(1);

        services.claimPendingPodiumGrants("u1");

        verify(walletRepository).addRolls("u1", GachaEconomy.GUILD_DAILY_1ST);
    }

    @Test
    @DisplayName("daily podium mutation is idempotent when grant already exists")
    void mutationsRejectDuplicateDailyPodiumGrant() {
        LocalDate yesterday = Utils.getCaliforniaDate().minusDays(1);
        given(discordPlayerGuildRepository.findGuildIdsByDiscordPlayerId("u1")).willReturn(List.of("guild-1"));
        given(isLockedServices.findLock(yesterday)).willReturn(Optional.of(
                IsLocked.builder().date(yesterday).lockTime(OffsetDateTime.now().minusHours(1)).build()));
        given(isLockedServices.findLock(argThat(d -> d != null && !d.equals(yesterday))))
                .willReturn(Optional.empty());
        given(dailyRosterRepository.getTodaysLeaderboardByGuildId("guild-1", yesterday)).willReturn(List.of(
                DailyRosterPlayer.builder().discordPlayerId("u1").fantasyScore(50.0).build()));
        given(grantRepository.tryInsertGrant(any(), eq(GachaEconomy.REASON_GUILD_DAILY_PODIUM), any(), anyInt()))
                .willReturn(0);

        services.claimPendingPodiumGrants("u1");

        verify(walletRepository, never()).addRolls(eq("u1"), eq(GachaEconomy.GUILD_DAILY_1ST));
    }

    @Test
    @DisplayName("weekly podium grants when prior week has settled")
    void weeklyPodiumGrantsSecondPlace() {
        LocalDate today = Utils.getCaliforniaDate();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        LocalDate weekEnd = weekStart.plusDays(6);
        given(discordPlayerGuildRepository.findGuildIdsByDiscordPlayerId("u1")).willReturn(List.of("guild-1"));
        // daily (yesterday) unsettled
        given(isLockedServices.findLock(today.minusDays(1))).willReturn(Optional.empty());
        given(isLockedServices.findLock(weekEnd)).willReturn(Optional.of(
                IsLocked.builder().date(weekEnd).lockTime(OffsetDateTime.now().minusHours(2)).build()));
        given(dailyRosterRepository.getWeeksLeaderboardByGuildId("guild-1", weekStart, weekEnd)).willReturn(List.of(
                DailyRosterPlayer.builder().discordPlayerId("top").fantasyScore(100.0).build(),
                DailyRosterPlayer.builder().discordPlayerId("u1").fantasyScore(80.0).build()));
        given(grantRepository.tryInsertGrant(
                "u1",
                GachaEconomy.REASON_GUILD_WEEKLY_PODIUM,
                "guild-1:" + weekStart,
                GachaEconomy.GUILD_WEEKLY_2ND)).willReturn(1);

        services.claimPendingPodiumGrants("u1");

        verify(walletRepository).addRolls("u1", GachaEconomy.GUILD_WEEKLY_2ND);
    }

    @Test
    @DisplayName("podium mutation skips users outside top three")
    void mutationsRejectPodiumOutsideTopThree() {
        LocalDate yesterday = Utils.getCaliforniaDate().minusDays(1);
        given(discordPlayerGuildRepository.findGuildIdsByDiscordPlayerId("u1")).willReturn(List.of("guild-1"));
        given(isLockedServices.findLock(yesterday)).willReturn(Optional.of(
                IsLocked.builder().date(yesterday).lockTime(OffsetDateTime.now().minusHours(1)).build()));
        given(isLockedServices.findLock(argThat(d -> d != null && !d.equals(yesterday))))
                .willReturn(Optional.empty());
        given(dailyRosterRepository.getTodaysLeaderboardByGuildId("guild-1", yesterday)).willReturn(List.of(
                DailyRosterPlayer.builder().discordPlayerId("a").fantasyScore(90.0).build(),
                DailyRosterPlayer.builder().discordPlayerId("b").fantasyScore(80.0).build(),
                DailyRosterPlayer.builder().discordPlayerId("c").fantasyScore(70.0).build(),
                DailyRosterPlayer.builder().discordPlayerId("u1").fantasyScore(10.0).build()));

        services.claimPendingPodiumGrants("u1");

        verify(grantRepository, never()).tryInsertGrant(eq("u1"), eq(GachaEconomy.REASON_GUILD_DAILY_PODIUM), any(), anyInt());
    }

    private void assertStreakBonus(int priorStreakDays, int expectedStreak, String reason, int amount) {
        reset(grantRepository, walletRepository, isLockedRepository);
        lenient()
                .when(grantRepository.tryInsertGrant(
                        anyString(),
                        eq(GachaEconomy.REASON_STARTER),
                        eq(GachaEconomy.PERIOD_STARTER),
                        eq(GachaEconomy.STARTER_ROLLS)))
                .thenReturn(0);
        LocalDate day = LocalDate.parse("2025-12-25");
        LocalDate prev = LocalDate.parse("2025-12-24");
        given(grantRepository.tryInsertGrant("u1", GachaEconomy.REASON_ROSTER_COMPLETE, day.toString(), 40))
                .willReturn(1);
        given(walletRepository.findWallet("u1")).willReturn(Optional.of(GachaUserWallet.builder()
                .discordPlayerId("u1")
                .streakDays(priorStreakDays)
                .streakLastDate(prev)
                .build()));
        given(isLockedRepository.findPreviousGameDay(day)).willReturn(Optional.of(
                IsLocked.builder().date(prev).lockTime(OffsetDateTime.now()).build()));
        given(grantRepository.tryInsertGrant("u1", reason, day.toString(), amount)).willReturn(1);

        assertTrue(services.tryRosterCompleteGrant("u1", day));
        verify(walletRepository).updateStreak("u1", expectedStreak, day);
        verify(walletRepository).addRolls("u1", amount);
    }

    private void stubOpenPull(UUID skinId, boolean alreadyOwned) {
        given(jdbcTemplate.execute(anyString(), any(PreparedStatementCallback.class))).willReturn(null);
        given(walletRepository.tryDebitRolls("u1", 100)).willReturn(1);
        given(gachaPoolService.loadWeightedPool()).willReturn(List.of(new WeightedPlayer(1L, 30, 30, "Test Player")));
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
