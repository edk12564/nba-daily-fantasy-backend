package com.picknroll.demo.services;

import com.picknroll.demo.gacha.GachaEconomy;
import com.picknroll.demo.gacha.GachaRarity;
import com.picknroll.demo.models.dtos.GachaSkin;
import com.picknroll.demo.models.dtos.GachaUserWallet;
import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.models.gacha.*;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.repositories.*;
import com.picknroll.demo.utils.Utils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class GachaServices {

    public static final String INSUFFICIENT_ROLLS = "INSUFFICIENT_ROLLS";
    public static final String EMPTY_POOL = "EMPTY_POOL";
    public static final String INVALID_COUNT = "INVALID_COUNT";

    private final GachaUserWalletRepository walletRepository;
    private final GachaSkinRepository skinRepository;
    private final GachaUserSkinRepository userSkinRepository;
    private final GachaTokenGrantRepository grantRepository;
    private final DiscordPlayerGuildRepository discordPlayerGuildRepository;
    private final DailyRosterRepository dailyRosterRepository;
    private final IsLockedRepository isLockedRepository;
    private final IsLockedServices isLockedServices;
    private final GachaPoolService gachaPoolService;
    private final JdbcTemplate jdbcTemplate;

    public GachaServices(
            GachaUserWalletRepository walletRepository,
            GachaSkinRepository skinRepository,
            GachaUserSkinRepository userSkinRepository,
            GachaTokenGrantRepository grantRepository,
            DiscordPlayerGuildRepository discordPlayerGuildRepository,
            DailyRosterRepository dailyRosterRepository,
            IsLockedRepository isLockedRepository,
            IsLockedServices isLockedServices,
            GachaPoolService gachaPoolService,
            JdbcTemplate jdbcTemplate) {
        this.walletRepository = walletRepository;
        this.skinRepository = skinRepository;
        this.userSkinRepository = userSkinRepository;
        this.grantRepository = grantRepository;
        this.discordPlayerGuildRepository = discordPlayerGuildRepository;
        this.dailyRosterRepository = dailyRosterRepository;
        this.isLockedRepository = isLockedRepository;
        this.isLockedServices = isLockedServices;
        this.gachaPoolService = gachaPoolService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public GachaWalletResponse getWallet(String discordPlayerId) {
        ensureWallet(discordPlayerId);
        claimPendingPodiumGrants(discordPlayerId);
        return toWalletResponse(requireWallet(discordPlayerId));
    }

    public List<GachaInventoryItem> getInventory(String discordPlayerId) {
        ensureWallet(discordPlayerId);
        List<GachaSkin> owned = skinRepository.findOwnedSkins(discordPlayerId);
        Map<Long, GachaRarity> maxByPlayer = new HashMap<>();
        for (GachaSkin skin : owned) {
            GachaRarity rarity = GachaRarity.fromString(skin.getRarity());
            maxByPlayer.merge(skin.getNbaPlayerId(), rarity, (a, b) -> a.rank() >= b.rank() ? a : b);
        }
        Map<Long, WeightedPlayer> latestById = latestPlayersById();
        List<GachaInventoryItem> items = new ArrayList<>();
        for (GachaSkin skin : owned) {
            GachaRarity rarity = GachaRarity.fromString(skin.getRarity());
            boolean display = rarity == maxByPlayer.get(skin.getNbaPlayerId());
            WeightedPlayer latest = latestById.get(skin.getNbaPlayerId());
            items.add(GachaInventoryItem.builder()
                    .skinId(skin.getSkinId())
                    .nbaPlayerId(skin.getNbaPlayerId())
                    .playerName(latest != null ? latest.playerName() : null)
                    .rarity(skin.getRarity())
                    .artUrl(skin.getArtUrl())
                    .frameKey(skin.getFrameKey())
                    .displaySkin(display)
                    .playerStar(GachaEconomy.playerStar(latest != null ? latest.dollarValue() : null))
                    .build());
        }
        return items;
    }

    public GachaRatesResponse getRates() {
        Map<String, Double> rates = new LinkedHashMap<>();
        Map<String, Integer> dust = new LinkedHashMap<>();
        for (GachaRarity rarity : GachaRarity.values()) {
            rates.put(rarity.name(), GachaEconomy.baseRates().get(rarity));
            dust.put(rarity.name(), GachaEconomy.dustFor(rarity));
        }
        return GachaRatesResponse.builder()
                .pullCost(GachaEconomy.PULL_COST)
                .rarityRates(rates)
                .dustOnDuplicate(dust)
                .softPityStart(GachaEconomy.SOFT_PITY_START)
                .softPityLegendaryBonusPerPull(GachaEconomy.SOFT_PITY_LEGENDARY_BONUS)
                .hardPityAt(GachaEconomy.HARD_PITY_AT)
                .weightFloor(GachaEconomy.WEIGHT_FLOOR)
                .build();
    }

    @Transactional
    public PullOutcome pull(String discordPlayerId, int count) {
        if (count != 1 && count != 10) {
            return PullOutcome.error(INVALID_COUNT);
        }
        ensureWallet(discordPlayerId);
        lockWallet(discordPlayerId);

        int cost = count * GachaEconomy.PULL_COST;
        int debited = walletRepository.tryDebitRolls(discordPlayerId, cost);
        if (debited != 1) {
            return PullOutcome.error(INSUFFICIENT_ROLLS);
        }

        List<WeightedPlayer> pool = gachaPoolService.loadWeightedPool();
        if (pool.isEmpty()) {
            walletRepository.addRolls(discordPlayerId, cost);
            return PullOutcome.error(EMPTY_POOL);
        }

        GachaUserWallet wallet = requireWallet(discordPlayerId);
        int pity = wallet.getPityCounter();
        List<GachaPullResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            pity += 1;
            PullStep step = performSinglePull(discordPlayerId, pool, pity, GachaEconomy.ratesForPity(pity), false);
            pity = step.pityAfter();
            results.add(step.result());
        }

        if (count == 10 && results.stream().noneMatch(r -> GachaRarity.fromString(r.getRarity()).isRareOrHigher())) {
            int commonIdx = -1;
            for (int i = 0; i < results.size(); i++) {
                if (GachaRarity.COMMON.name().equals(results.get(i).getRarity())) {
                    commonIdx = i;
                    break;
                }
            }
            if (commonIdx >= 0) {
                reverseResult(discordPlayerId, results.get(commonIdx));
                PullStep replacement = performSinglePull(
                        discordPlayerId, pool, pity, GachaEconomy.rarePlusRates(), true);
                pity = replacement.pityAfter();
                results.set(commonIdx, replacement.result());
            }
        }

        walletRepository.setPityCounter(discordPlayerId, pity);
        GachaUserWallet updated = requireWallet(discordPlayerId);
        return PullOutcome.ok(GachaPullResponse.builder()
                .results(results)
                .rolls(updated.getRolls())
                .dust(updated.getDust())
                .pityCounter(updated.getPityCounter())
                .build());
    }

    @Transactional
    public boolean tryRosterCompleteGrant(String discordPlayerId, LocalDate playDay) {
        ensureWallet(discordPlayerId);
        String periodKey = playDay.toString();
        boolean granted = tryGrant(
                discordPlayerId,
                GachaEconomy.REASON_ROSTER_COMPLETE,
                periodKey,
                GachaEconomy.ROSTER_COMPLETE_ROLLS);
        if (!granted) {
            return false;
        }
        updateStreakAfterRosterComplete(discordPlayerId, playDay);
        return true;
    }

    void claimPendingPodiumGrants(String discordPlayerId) {
        LocalDate today = Utils.getCaliforniaDate();
        LocalDate yesterday = today.minusDays(1);
        List<String> guildIds = discordPlayerGuildRepository.findGuildIdsByDiscordPlayerId(discordPlayerId);
        for (String guildId : guildIds) {
            maybeGrantDailyPodium(discordPlayerId, guildId, yesterday);
            LocalDate priorWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
            LocalDate priorWeekEnd = priorWeekStart.plusDays(6);
            maybeGrantWeeklyPodium(discordPlayerId, guildId, priorWeekStart, priorWeekEnd);
        }
    }

    private void maybeGrantDailyPodium(String discordPlayerId, String guildId, LocalDate date) {
        if (!scoresSettled(date)) {
            return;
        }
        int rank = rankOnDailyGuildBoard(guildId, date, discordPlayerId);
        int amount = GachaEconomy.dailyPodiumAmount(rank);
        if (amount <= 0) {
            return;
        }
        tryGrant(
                discordPlayerId,
                GachaEconomy.REASON_GUILD_DAILY_PODIUM,
                guildId + ":" + date,
                amount);
    }

    private void maybeGrantWeeklyPodium(
            String discordPlayerId, String guildId, LocalDate weekStart, LocalDate weekEnd) {
        if (!scoresSettled(weekEnd)) {
            return;
        }
        int rank = rankOnWeeklyGuildBoard(guildId, weekStart, weekEnd, discordPlayerId);
        int amount = GachaEconomy.weeklyPodiumAmount(rank);
        if (amount <= 0) {
            return;
        }
        tryGrant(
                discordPlayerId,
                GachaEconomy.REASON_GUILD_WEEKLY_PODIUM,
                guildId + ":" + weekStart,
                amount);
    }

    private boolean scoresSettled(LocalDate date) {
        Optional<IsLocked> lock = isLockedServices.findLock(date);
        if (lock.isEmpty() || lock.get().getLockTime() == null) {
            return false;
        }
        return !lock.get().getLockTime().isAfter(OffsetDateTime.now());
    }

    private int rankOnDailyGuildBoard(String guildId, LocalDate date, String discordPlayerId) {
        List<DailyRosterPlayer> rows = dailyRosterRepository.getTodaysLeaderboardByGuildId(guildId, date);
        return rankFromPlayerRows(rows, discordPlayerId);
    }

    private int rankOnWeeklyGuildBoard(
            String guildId, LocalDate start, LocalDate end, String discordPlayerId) {
        List<DailyRosterPlayer> rows = dailyRosterRepository.getWeeksLeaderboardByGuildId(guildId, start, end);
        LinkedHashMap<String, Double> totals = new LinkedHashMap<>();
        for (DailyRosterPlayer row : rows) {
            totals.merge(
                    row.getDiscordPlayerId(),
                    row.getFantasyScore() == null ? 0.0 : row.getFantasyScore(),
                    Double::sum);
        }
        int rank = 0;
        for (String id : totals.keySet()) {
            rank++;
            if (id.equals(discordPlayerId)) {
                return rank <= 3 ? rank : 0;
            }
        }
        return 0;
    }

    private int rankFromPlayerRows(List<DailyRosterPlayer> rows, String discordPlayerId) {
        LinkedHashMap<String, Double> totals = new LinkedHashMap<>();
        for (DailyRosterPlayer row : rows) {
            totals.merge(
                    row.getDiscordPlayerId(),
                    row.getFantasyScore() == null ? 0.0 : row.getFantasyScore(),
                    Double::sum);
        }
        List<Map.Entry<String, Double>> ranked = new ArrayList<>(totals.entrySet());
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < ranked.size() && i < 3; i++) {
            if (ranked.get(i).getKey().equals(discordPlayerId)) {
                return i + 1;
            }
        }
        return 0;
    }

    private void updateStreakAfterRosterComplete(String discordPlayerId, LocalDate playDay) {
        GachaUserWallet wallet = requireWallet(discordPlayerId);
        int streakDays = 1;
        Optional<IsLocked> previousGame = isLockedRepository.findPreviousGameDay(playDay);
        if (previousGame.isPresent()
                && wallet.getStreakLastDate() != null
                && wallet.getStreakLastDate().equals(previousGame.get().getDate())) {
            streakDays = wallet.getStreakDays() + 1;
        }
        walletRepository.updateStreak(discordPlayerId, streakDays, playDay);

        String periodKey = playDay.toString();
        if (streakDays == 3) {
            tryGrant(discordPlayerId, GachaEconomy.REASON_STREAK_3, periodKey, GachaEconomy.STREAK_3_ROLLS);
        } else if (streakDays == 7) {
            tryGrant(discordPlayerId, GachaEconomy.REASON_STREAK_7, periodKey, GachaEconomy.STREAK_7_ROLLS);
        } else if (streakDays == 14) {
            tryGrant(discordPlayerId, GachaEconomy.REASON_STREAK_14, periodKey, GachaEconomy.STREAK_14_ROLLS);
        }
    }

    private boolean tryGrant(String discordPlayerId, String reason, String periodKey, int amount) {
        int inserted = grantRepository.tryInsertGrant(discordPlayerId, reason, periodKey, amount);
        if (inserted != 1) {
            return false;
        }
        walletRepository.addRolls(discordPlayerId, amount);
        return true;
    }

    private PullStep performSinglePull(
            String discordPlayerId,
            List<WeightedPlayer> pool,
            int pityAfterIncrement,
            Map<GachaRarity, Double> rates,
            boolean rarePlusReplacement) {
        GachaRarity rarity = rollRarity(rates);
        WeightedPlayer player = weightedDraw(pool);
        GachaSkin skin = skinRepository
                .findByNbaPlayerIdAndRarity(player.nbaPlayerId(), rarity.name())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing gacha_skins row for player " + player.nbaPlayerId() + " " + rarity));

        int pityAfter = pityAfterIncrement;
        if (rarity == GachaRarity.LEGENDARY) {
            pityAfter = 0;
        } else if (rarePlusReplacement) {
            // pity already counted on the reversed common pull; keep current counter unless legendary
            pityAfter = pityAfterIncrement;
        }

        boolean owned = userSkinRepository.countOwnership(discordPlayerId, skin.getSkinId()) > 0;
        if (owned) {
            int dust = GachaEconomy.dustFor(rarity);
            walletRepository.addDust(discordPlayerId, dust);
            return new PullStep(pityAfter, GachaPullResult.builder()
                    .outcome("DUPLICATE")
                    .rarity(rarity.name())
                    .nbaPlayerId(skin.getNbaPlayerId())
                    .playerName(player.playerName())
                    .skinId(skin.getSkinId())
                    .artUrl(skin.getArtUrl())
                    .frameKey(skin.getFrameKey())
                    .dustGranted(dust)
                    .playerStar(GachaEconomy.playerStar(player.dollarValue()))
                    .build());
        }

        userSkinRepository.insertOwnership(discordPlayerId, skin.getSkinId());
        return new PullStep(pityAfter, GachaPullResult.builder()
                .outcome("NEW")
                .rarity(rarity.name())
                .nbaPlayerId(skin.getNbaPlayerId())
                .playerName(player.playerName())
                .skinId(skin.getSkinId())
                .artUrl(skin.getArtUrl())
                .frameKey(skin.getFrameKey())
                .dustGranted(0)
                .playerStar(GachaEconomy.playerStar(player.dollarValue()))
                .build());
    }

    private void reverseResult(String discordPlayerId, GachaPullResult result) {
        if ("NEW".equals(result.getOutcome())) {
            userSkinRepository.deleteOwnership(discordPlayerId, result.getSkinId());
        } else if ("DUPLICATE".equals(result.getOutcome()) && result.getDustGranted() > 0) {
            walletRepository.trySubtractDust(discordPlayerId, result.getDustGranted());
        }
    }

    GachaRarity rollRarity(Map<GachaRarity, Double> rates) {
        double roll = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0.0;
        for (GachaRarity rarity : List.of(
                GachaRarity.LEGENDARY, GachaRarity.EPIC, GachaRarity.RARE, GachaRarity.COMMON)) {
            cumulative += rates.getOrDefault(rarity, 0.0);
            if (roll < cumulative) {
                return rarity;
            }
        }
        return GachaRarity.COMMON;
    }

    WeightedPlayer weightedDraw(List<WeightedPlayer> pool) {
        long total = 0;
        for (WeightedPlayer player : pool) {
            total += player.weight();
        }
        long pick = ThreadLocalRandom.current().nextLong(total);
        long running = 0;
        for (WeightedPlayer player : pool) {
            running += player.weight();
            if (pick < running) {
                return player;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private Map<Long, WeightedPlayer> latestPlayersById() {
        return gachaPoolService.loadWeightedPool().stream()
                .collect(Collectors.toMap(WeightedPlayer::nbaPlayerId, p -> p, (a, b) -> a));
    }

    private void ensureWallet(String discordPlayerId) {
        walletRepository.ensureWallet(discordPlayerId);
    }

    private GachaUserWallet requireWallet(String discordPlayerId) {
        return walletRepository.findWallet(discordPlayerId)
                .orElseThrow(() -> new IllegalStateException("Wallet missing for " + discordPlayerId));
    }

    private GachaWalletResponse toWalletResponse(GachaUserWallet wallet) {
        return GachaWalletResponse.builder()
                .rolls(wallet.getRolls())
                .dust(wallet.getDust())
                .pityCounter(wallet.getPityCounter())
                .streakDays(wallet.getStreakDays())
                .streakLastDate(wallet.getStreakLastDate())
                .build();
    }

    private void lockWallet(String discordPlayerId) {
        jdbcTemplate.execute(
                "SELECT pg_advisory_xact_lock(hashtext(?), hashtext('gacha'))",
                (PreparedStatementCallback<Void>) statement -> {
                    statement.setString(1, discordPlayerId);
                    statement.execute();
                    return null;
                });
    }

    public record PullOutcome(boolean success, String error, GachaPullResponse response) {
        public static PullOutcome ok(GachaPullResponse response) {
            return new PullOutcome(true, null, response);
        }

        public static PullOutcome error(String error) {
            return new PullOutcome(false, error, null);
        }
    }

    private record PullStep(int pityAfter, GachaPullResult result) {
    }
}
