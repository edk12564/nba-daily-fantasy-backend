package com.picknroll.demo.repositories;

import com.picknroll.demo.gacha.GachaEconomy;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.services.GachaServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GachaRepositoryIT extends AbstractIntegrationTest {

    private static final LocalDate TEST_DATE = LocalDate.parse("2025-12-25");

    @Autowired
    GachaServices gachaServices;
    @Autowired
    GachaUserWalletRepository walletRepository;
    @Autowired
    GachaUserSkinRepository userSkinRepository;
    @Autowired
    DailyRosterRepository dailyRosterRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void pullDebitsRollsAndOwnsOrDusts() {
        walletRepository.ensureWallet("gacha-it-user", GachaEconomy.STARTER_ROLLS);

        GachaServices.PullOutcome first = gachaServices.pull("gacha-it-user", 1);
        assertTrue(first.success(), first.error());
        assertEquals(GachaEconomy.STARTER_ROLLS - GachaEconomy.PULL_COST, first.response().getRolls());
        assertEquals(1, first.response().getResults().size());

        // Force duplicate path: pull until we get a dupe or burn remaining budget with same pity path
        // Seed ownership of all skins for player 1 then pull again after topping rolls
        List<Map<String, Object>> skins = jdbcTemplate.queryForList(
                "SELECT skin_id FROM gacha_skins WHERE nba_player_id = 1");
        assertFalse(skins.isEmpty());
        for (Map<String, Object> row : skins) {
            userSkinRepository.insertOwnership("gacha-it-user", (UUID) row.get("skin_id"));
        }
        // Own all catalog skins → every pull is dust
        List<Map<String, Object>> allSkins = jdbcTemplate.queryForList("SELECT skin_id FROM gacha_skins");
        for (Map<String, Object> row : allSkins) {
            userSkinRepository.insertOwnership("gacha-it-user", (UUID) row.get("skin_id"));
        }
        walletRepository.addRolls("gacha-it-user", 100);
        GachaServices.PullOutcome dupe = gachaServices.pull("gacha-it-user", 1);
        assertTrue(dupe.success());
        assertEquals("DUPLICATE", dupe.response().getResults().get(0).getOutcome());
        assertTrue(dupe.response().getResults().get(0).getDustGranted() > 0);
    }

    @Test
    void rosterQueryIncludesEquippedSkinFields() {
        UUID skinId = jdbcTemplate.queryForObject(
                "SELECT skin_id FROM gacha_skins WHERE nba_player_id = 1 AND rarity = 'EPIC'",
                UUID.class);
        userSkinRepository.insertOwnership("player-1", skinId);

        List<DailyRosterPlayer> roster = dailyRosterRepository.getTodaysRosterByDiscordId("player-1", TEST_DATE);
        DailyRosterPlayer lebron = roster.stream()
                .filter(p -> p.getNbaPlayerId() != null && p.getNbaPlayerId() == 1L)
                .findFirst()
                .orElseThrow();
        assertEquals("EPIC", lebron.getEquippedSkinRarity());
        assertEquals("frame_epic", lebron.getFrameKey());
        assertNotNull(lebron.getEquippedArtUrl());
        assertNotNull(lebron.getPlayerStar());
    }

    @Test
    void insufficientRollsFailsClosed() {
        walletRepository.ensureWallet("broke-user", GachaEconomy.STARTER_ROLLS);
        jdbcTemplate.update(
                "UPDATE gacha_user_wallet SET rolls = 0 WHERE discord_player_id = ?",
                "broke-user");
        GachaServices.PullOutcome outcome = gachaServices.pull("broke-user", 1);
        assertFalse(outcome.success());
        assertEquals(GachaServices.INSUFFICIENT_ROLLS, outcome.error());
    }

    @Test
    void newWalletStartsWithStarterRolls() {
        walletRepository.ensureWallet("starter-user", GachaEconomy.STARTER_ROLLS);
        Integer rolls = jdbcTemplate.queryForObject(
                "SELECT rolls FROM gacha_user_wallet WHERE discord_player_id = ?",
                Integer.class,
                "starter-user");
        assertEquals(GachaEconomy.STARTER_ROLLS, rolls);
        // Idempotent — second ensure must not double-credit
        walletRepository.ensureWallet("starter-user", GachaEconomy.STARTER_ROLLS);
        Integer again = jdbcTemplate.queryForObject(
                "SELECT rolls FROM gacha_user_wallet WHERE discord_player_id = ?",
                Integer.class,
                "starter-user");
        assertEquals(GachaEconomy.STARTER_ROLLS, again);
    }
}
