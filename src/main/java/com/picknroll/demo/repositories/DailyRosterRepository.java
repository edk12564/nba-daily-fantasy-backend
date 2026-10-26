package com.picknroll.demo.repositories;

import com.picknroll.demo.models.dtos.DailyRoster;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DailyRosterRepository extends CrudRepository<DailyRoster, UUID> {

    /* Save Player */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO daily_roster (discord_player_id, nba_player_uid, date, nickname, position)
            SELECT
                CAST(:discordPlayerId AS TEXT) as discord_player_id,
                CAST(:nbaPlayerUid AS UUID) as nba_player_uid,
                CAST(:date AS DATE) as date,
                CAST(:nickname AS TEXT) as nickname,
                CAST(:position AS daily_roster_position) as position
            FROM
                nba_players np
            WHERE np.nba_player_uid = :nbaPlayerUid
                  AND np.date = CAST(:date as VARCHAR)
                  AND np.position = :position
            ON CONFLICT(discord_player_id, date, position)
            DO UPDATE SET
                nba_player_uid = EXCLUDED.nba_player_uid,
                position = CAST(:position AS daily_roster_position),
                nickname = CAST(:nickname AS TEXT)
            """)
    Integer saveRosterChoice(UUID nbaPlayerUid, String discordPlayerId, String nickname, String position,
            String date);

    /* Delete Player */
    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM daily_roster dr
            WHERE dr.date = :date
              AND dr.discord_player_id = :discordId
              AND dr.nba_player_uid = :nbaPlayerUid
            """)
    void deleteRosterPlayerByDateAndDiscordIdAndPlayerName(LocalDate date, String discordId, UUID nbaPlayerUid);


    /* Get Roster */
    @Query(value = """
                    SELECT
                        dr.discord_player_id,
                        np.nba_player_id,
                        dr.nba_player_uid,
                        dr.date,
                        dr.nickname,
                        dr.position AS position,
                        np.name,
                        np.dollar_value,
                        np.fantasy_score,
                        equipped.rarity AS equipped_skin_rarity,
                        equipped.art_url AS equipped_art_url,
                        equipped.frame_key AS frame_key,
                        CASE
                            WHEN np.dollar_value IS NULL OR np.dollar_value <= 14 THEN 1
                            WHEN np.dollar_value <= 19 THEN 2
                            WHEN np.dollar_value <= 24 THEN 3
                            WHEN np.dollar_value <= 28 THEN 4
                            ELSE 5
                        END AS player_star
                    FROM daily_roster dr
                    JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
                    LEFT JOIN LATERAL (
                        SELECT gs.rarity, gs.art_url, gs.frame_key
                        FROM gacha_user_skins gus
                        JOIN gacha_skins gs ON gs.skin_id = gus.skin_id
                        WHERE gus.discord_player_id = dr.discord_player_id
                          AND gs.nba_player_id = np.nba_player_id
                        ORDER BY CASE gs.rarity
                            WHEN 'LEGENDARY' THEN 4
                            WHEN 'EPIC' THEN 3
                            WHEN 'RARE' THEN 2
                            ELSE 1
                        END DESC
                        LIMIT 1
                    ) equipped ON true
                    WHERE dr.discord_player_id = :discordId AND dr.date = :date
            """)
    List<DailyRosterPlayer> getTodaysRosterByDiscordId(String discordId, LocalDate date);

    /* Get Price — billed salary (face − skin discount, floor 1). Owner = :discordId. */
    // TODO: What happens if the player is already on the roster?
    @Query(value = """
            SELECT GREATEST(1, np.dollar_value - COALESCE(disc.amount, 0))
            FROM daily_roster dr
            JOIN nba_players np ON np.nba_player_uid = dr.nba_player_uid
            LEFT JOIN LATERAL (
                SELECT CASE gs.rarity
                    WHEN 'LEGENDARY' THEN 3
                    WHEN 'EPIC' THEN 2
                    WHEN 'RARE' THEN 1
                    ELSE 0
                END AS amount
                FROM gacha_user_skins gus
                JOIN gacha_skins gs ON gs.skin_id = gus.skin_id
                WHERE gus.discord_player_id = :discordId
                  AND gs.nba_player_id = np.nba_player_id
                ORDER BY CASE gs.rarity
                    WHEN 'LEGENDARY' THEN 4
                    WHEN 'EPIC' THEN 3
                    WHEN 'RARE' THEN 2
                    ELSE 1
                END DESC
                LIMIT 1
            ) disc ON true
            WHERE dr.discord_player_id = :discordId
              AND dr.date = CAST(:date AS DATE)
              AND dr.position <> :position::daily_roster_position
            UNION ALL
            SELECT GREATEST(1, np.dollar_value - COALESCE(disc.amount, 0))
            FROM nba_players np
            LEFT JOIN LATERAL (
                SELECT CASE gs.rarity
                    WHEN 'LEGENDARY' THEN 3
                    WHEN 'EPIC' THEN 2
                    WHEN 'RARE' THEN 1
                    ELSE 0
                END AS amount
                FROM gacha_user_skins gus
                JOIN gacha_skins gs ON gs.skin_id = gus.skin_id
                WHERE gus.discord_player_id = :discordId
                  AND gs.nba_player_id = np.nba_player_id
                ORDER BY CASE gs.rarity
                    WHEN 'LEGENDARY' THEN 4
                    WHEN 'EPIC' THEN 3
                    WHEN 'RARE' THEN 2
                    ELSE 1
                END DESC
                LIMIT 1
            ) disc ON true
            WHERE np.nba_player_uid = :nbaPlayerUid
              AND np.date = CAST(:date AS VARCHAR)
            """)
    List<Integer> getTodaysRosterPriceWithPlayer(String discordId, String position, String date, UUID nbaPlayerUid);

    // TODO: Race condition logic here? We need to combine save player and get
    // roster in the same query.

    /* Leaderboards */
    @Query(value = """
            WITH roster_totals AS (
                SELECT
                    dr.discord_player_id,
                    SUM(COALESCE(np.fantasy_score, 0)) AS total_score
                FROM daily_roster dr
                JOIN nba_players np ON np.nba_player_uid = dr.nba_player_uid
                    AND np.date = CAST(dr.date AS VARCHAR)
                WHERE dr.date = :date
                GROUP BY dr.discord_player_id
                ORDER BY total_score DESC
                LIMIT 100
            )
            SELECT
                dr.discord_player_id,
                np.nba_player_id,
                dr.nba_player_uid,
                dr.date,
                dr.nickname,
                dr.position AS position,
                np.name,
                np.dollar_value,
                np.fantasy_score AS fantasy_score,
                equipped.rarity AS equipped_skin_rarity,
                equipped.art_url AS equipped_art_url,
                equipped.frame_key AS frame_key,
                CASE
                    WHEN np.dollar_value IS NULL OR np.dollar_value <= 14 THEN 1
                    WHEN np.dollar_value <= 19 THEN 2
                    WHEN np.dollar_value <= 24 THEN 3
                    WHEN np.dollar_value <= 28 THEN 4
                    ELSE 5
                END AS player_star
            FROM daily_roster dr
            JOIN nba_players np ON np.nba_player_uid = dr.nba_player_uid
                AND np.date = CAST(dr.date AS VARCHAR)
            JOIN roster_totals rr ON dr.discord_player_id = rr.discord_player_id
            LEFT JOIN LATERAL (
                SELECT gs.rarity, gs.art_url, gs.frame_key
                FROM gacha_user_skins gus
                JOIN gacha_skins gs ON gs.skin_id = gus.skin_id
                WHERE gus.discord_player_id = dr.discord_player_id
                  AND gs.nba_player_id = np.nba_player_id
                ORDER BY CASE gs.rarity
                    WHEN 'LEGENDARY' THEN 4
                    WHEN 'EPIC' THEN 3
                    WHEN 'RARE' THEN 2
                    ELSE 1
                END DESC
                LIMIT 1
            ) equipped ON true
            WHERE dr.date = :date
            ORDER BY rr.total_score DESC, np.fantasy_score DESC
            """)
    List<DailyRosterPlayer> getTodaysGlobalLeaderboard(LocalDate date);

    @Query(value = """
            SELECT dr.discord_player_id, dr.nickname, sum(np.fantasy_score) as fantasy_score
            FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
            JOIN discord_player_guilds dpg on dpg.discord_player_id = dr.discord_player_id
            WHERE dpg.guild_id = :guildId and dr.date <= :endDay AND dr.date >= :startDay
            GROUP BY dr.discord_player_id, dr.nickname
            ORDER BY fantasy_score DESC
            LIMIT 100
            """)
    List<DailyRosterPlayer> getWeeksLeaderboardByGuildId(String guildId, LocalDate startDay, LocalDate endDay);

    @Query(value = """
            SELECT dr.discord_player_id, dr.nickname, sum(np.fantasy_score) as fantasy_score
            FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
            WHERE dr.date <= :endDay AND dr.date >= :startDay
            GROUP BY dr.discord_player_id, dr.nickname
            ORDER BY fantasy_score DESC
            LIMIT 100
            """)
    List<DailyRosterPlayer> getWeeksGlobalLeaderboard(LocalDate startDay, LocalDate endDay);

    // TODO: group by discord player id
    @Query(value = """
            WITH roster_totals AS (
                    SELECT
                        dr.discord_player_id,
                        SUM(COALESCE(np.fantasy_score, 0)) AS total_score
                    FROM daily_roster dr
                    JOIN nba_players np ON np.nba_player_uid = dr.nba_player_uid
                        AND np.date = CAST(dr.date AS VARCHAR)
                    JOIN discord_player_guilds dpg on dpg.discord_player_id = dr.discord_player_id
                    WHERE dr.date = :date AND dpg.guild_id = :guildId
                    GROUP BY dr.discord_player_id
                    ORDER BY total_score DESC
                    LIMIT 100
                )

                    SELECT
                        dr.discord_player_id,
                        np.nba_player_id,
                        dr.nba_player_uid,
                        dr.date,
                        dr.nickname,
                        dr.position AS position,
                        np.name,
                        np.dollar_value,
                        np.fantasy_score,
                        equipped.rarity AS equipped_skin_rarity,
                        equipped.art_url AS equipped_art_url,
                        equipped.frame_key AS frame_key,
                        CASE
                            WHEN np.dollar_value IS NULL OR np.dollar_value <= 14 THEN 1
                            WHEN np.dollar_value <= 19 THEN 2
                            WHEN np.dollar_value <= 24 THEN 3
                            WHEN np.dollar_value <= 28 THEN 4
                            ELSE 5
                        END AS player_star
                    FROM daily_roster dr
                    JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
                    JOIN discord_player_guilds dpg on dpg.discord_player_id = dr.discord_player_id
                    JOIN roster_totals rr ON dr.discord_player_id = rr.discord_player_id
                    LEFT JOIN LATERAL (
                        SELECT gs.rarity, gs.art_url, gs.frame_key
                        FROM gacha_user_skins gus
                        JOIN gacha_skins gs ON gs.skin_id = gus.skin_id
                        WHERE gus.discord_player_id = dr.discord_player_id
                          AND gs.nba_player_id = np.nba_player_id
                        ORDER BY CASE gs.rarity
                            WHEN 'LEGENDARY' THEN 4
                            WHEN 'EPIC' THEN 3
                            WHEN 'RARE' THEN 2
                            ELSE 1
                        END DESC
                        LIMIT 1
                    ) equipped ON true
                    WHERE dr.date = :date and dpg.guild_id = :guildId
                    ORDER BY rr.total_score DESC, np.fantasy_score DESC
                    """)
    List<DailyRosterPlayer> getTodaysLeaderboardByGuildId(String guildId, LocalDate date);

    /**
     * Best podium place (1–3) for this player across all guilds they belong to on {@code date}.
     * Null when they are outside the top three everywhere, guild boards are unscored, or they
     * have no guilds. Multi-guild membership cannot stack — only the best rank is returned.
     */
    @Query(value = """
            WITH my_guilds AS (
                SELECT guild_id
                FROM discord_player_guilds
                WHERE discord_player_id = :discordPlayerId
            ),
            guild_player_totals AS (
                SELECT
                    dpg.guild_id,
                    dr.discord_player_id,
                    SUM(COALESCE(np.fantasy_score, 0)) AS total_score,
                    BOOL_OR(np.fantasy_score IS NOT NULL) AS any_score_loaded
                FROM daily_roster dr
                JOIN nba_players np ON np.nba_player_uid = dr.nba_player_uid
                    AND np.date = CAST(dr.date AS VARCHAR)
                JOIN discord_player_guilds dpg ON dpg.discord_player_id = dr.discord_player_id
                JOIN my_guilds mg ON mg.guild_id = dpg.guild_id
                WHERE dr.date = :date
                GROUP BY dpg.guild_id, dr.discord_player_id
            ),
            eligible_guilds AS (
                SELECT guild_id
                FROM guild_player_totals
                GROUP BY guild_id
                HAVING BOOL_OR(any_score_loaded)
                   AND MAX(total_score) > 0
            ),
            ranked AS (
                SELECT
                    gpt.discord_player_id,
                    ROW_NUMBER() OVER (
                        PARTITION BY gpt.guild_id
                        ORDER BY gpt.total_score DESC, gpt.discord_player_id
                    ) AS place
                FROM guild_player_totals gpt
                JOIN eligible_guilds eg ON eg.guild_id = gpt.guild_id
            )
            SELECT MIN(place)::int
            FROM ranked
            WHERE discord_player_id = :discordPlayerId
              AND place <= 3
            """)
    Integer bestDailyGuildPodiumRank(String discordPlayerId, LocalDate date);

    /**
     * Best weekly podium place (1–3) across the player's guilds for {@code [startDay, endDay]}.
     * Same stacking / unscored rules as {@link #bestDailyGuildPodiumRank}.
     */
    @Query(value = """
            WITH my_guilds AS (
                SELECT guild_id
                FROM discord_player_guilds
                WHERE discord_player_id = :discordPlayerId
            ),
            guild_player_totals AS (
                SELECT
                    dpg.guild_id,
                    dr.discord_player_id,
                    SUM(COALESCE(np.fantasy_score, 0)) AS total_score,
                    BOOL_OR(np.fantasy_score IS NOT NULL) AS any_score_loaded
                FROM daily_roster dr
                JOIN nba_players np ON np.nba_player_uid = dr.nba_player_uid
                JOIN discord_player_guilds dpg ON dpg.discord_player_id = dr.discord_player_id
                JOIN my_guilds mg ON mg.guild_id = dpg.guild_id
                WHERE dr.date >= :startDay AND dr.date <= :endDay
                GROUP BY dpg.guild_id, dr.discord_player_id
            ),
            eligible_guilds AS (
                SELECT guild_id
                FROM guild_player_totals
                GROUP BY guild_id
                HAVING BOOL_OR(any_score_loaded)
                   AND MAX(total_score) > 0
            ),
            ranked AS (
                SELECT
                    gpt.discord_player_id,
                    ROW_NUMBER() OVER (
                        PARTITION BY gpt.guild_id
                        ORDER BY gpt.total_score DESC, gpt.discord_player_id
                    ) AS place
                FROM guild_player_totals gpt
                JOIN eligible_guilds eg ON eg.guild_id = gpt.guild_id
            )
            SELECT MIN(place)::int
            FROM ranked
            WHERE discord_player_id = :discordPlayerId
              AND place <= 3
            """)
    Integer bestWeeklyGuildPodiumRank(String discordPlayerId, LocalDate startDay, LocalDate endDay);

}
