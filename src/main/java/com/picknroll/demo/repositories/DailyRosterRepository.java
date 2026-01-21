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
                    SELECT dr.*, np.nba_player_uid, np.nba_player_id, np.name, np.dollar_value, np.fantasy_score
                    FROM daily_roster dr
                    JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
                    WHERE dr.discord_player_id = :discordId AND dr.date = :date
            """)
    List<DailyRosterPlayer> getTodaysRosterByDiscordId(String discordId, LocalDate date);

    /* Get Price */
    // TODO: What happens if the player is already on the roster?
    @Query(value = """
            SELECT np.dollar_value FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
                    WHERE dr.discord_player_id = :discordId AND dr.date = CAST(:date as DATE)
                                          AND dr.position <> :position::daily_roster_position
            UNION ALL
                SELECT np.dollar_value FROM nba_players np
                WHERE np.nba_player_uid = :nbaPlayerUid and np.date = CAST(:date as VARCHAR)
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
                np.fantasy_score AS fantasy_score
            FROM daily_roster dr
            JOIN nba_players np ON np.nba_player_uid = dr.nba_player_uid
                AND np.date = CAST(dr.date AS VARCHAR)
            JOIN roster_totals rr ON dr.discord_player_id = rr.discord_player_id
            WHERE dr.date = :date
            ORDER BY rr.total_score DESC, np.fantasy_score DESC
            """)
    List<DailyRosterPlayer> getTodaysGlobalLeaderboard(LocalDate date);

    @Query(value = """
            SELECT dr.discord_player_id, dr.nickname, sum(np.fantasy_score) as fantasy_score
            FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
            JOIN discord_player_guilds dpg on dpg.discord_player_id = dr.discord_player_id
            WHERE dpg.guild_id = :guildId and dr.date < :endDay AND dr.date > :startDay
            GROUP BY dr.discord_player_id, dr.nickname
            ORDER BY fantasy_score DESC
            LIMIT 100
            """)
    List<DailyRosterPlayer> getWeeksLeaderboardByGuildId(String guildId, LocalDate startDay, LocalDate endDay);

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

                    SELECT dr.discord_player_id, np.nba_player_id, dr.nba_player_uid, dr.date, dr.nickname, dr.position AS position, np.name, np.dollar_value, np.fantasy_score
                    FROM daily_roster dr
                    JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
                    JOIN discord_player_guilds dpg on dpg.discord_player_id = dr.discord_player_id
                    JOIN roster_totals rr ON dr.discord_player_id = rr.discord_player_id
                    WHERE dr.date = :date and dpg.guild_id = :guildId
                    ORDER BY rr.total_score DESC, np.fantasy_score DESC
                    """)
    List<DailyRosterPlayer> getTodaysLeaderboardByGuildId(String guildId, LocalDate date);

}
