package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.dtos.DailyRoster;
import com.bigschlong.demo.models.joinTables.DailyRosterPlayer;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Repository
public interface DailyRosterRepository extends CrudRepository<DailyRoster, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO daily_roster (discord_player_id, nba_player_uid, guild_id, date, nickname, position)
                            VALUES (:discordPlayerId, :nbaPlayerUid, :guildId, :date, :nickname, :position::daily_roster_position)
                            ON CONFLICT(discord_player_id, guild_id, date, position) DO UPDATE
                            SET nba_player_uid = :nbaPlayerUid, position = :position::daily_roster_position
            """)
    void saveRosterChoice(UUID nbaPlayerUid, String discordPlayerId, String guildId, String nickname, String position, LocalDate date);

    @Query(value = """
                    SELECT dr.*, np.nba_player_uid, np.nba_player_id, np.name, np.dollar_value FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
                    WHERE dr.discord_player_id = :discordId AND dr.guild_id = :guildId AND dr.date = :date
            """)
    List<DailyRosterPlayer> getTodaysRosterByDiscordIdAndGuildId(String discordId, String guildId, LocalDate date);

    @Query(value = """
            SELECT dr.discord_player_id, dr.nba_player_uid, dr.guild_id, dr.date, dr.nickname, dr.position AS position, np.name, np.dollar_value
            FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
            WHERE dr.guild_id = :guildId AND dr.date = CURRENT_DATE
            ORDER BY dr.nickname
            """)
    List<DailyRosterPlayer> getTodaysRostersByGuildId(String guildId);

    @Query(value = """
                    SELECT dr.discord_player_id, np.nba_player_id, dr.nba_player_uid, dr.guild_id, dr.date, dr.nickname, dr.position AS position, np.name, np.dollar_value, np.fantasy_score 
            FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
                    WHERE dr.guild_id = :guildId AND dr.date = :date
            ORDER BY np.fantasy_score DESC
            LIMIT 100
            """)
    List<DailyRosterPlayer> getTodaysRostersByGuildIdWithFantasyScore(String guildId, LocalDate date);

    @Query(value = """
            WITH roster_totals AS (
                                   SELECT
                                     dr.discord_player_id,
                                     dr.guild_id,
                                     SUM(np.fantasy_score) AS total_score
                                   FROM daily_roster dr
                                   JOIN nba_players np ON np.nba_player_uid = dr.nba_player_uid
                                   WHERE dr.date = :date
                                   GROUP BY dr.discord_player_id, dr.guild_id
                                 ),
                                 ranked_rosters AS (
                                   SELECT
                                     discord_player_id,
                                     guild_id,
                                     total_score,
                                     ROW_NUMBER() OVER (PARTITION BY discord_player_id ORDER BY total_score DESC) AS rn
                                   FROM roster_totals
                                 )
                                 SELECT
                                   dr.discord_player_id,
                                   np.nba_player_id,
                                   dr.nba_player_uid,
                                   dr.guild_id,
                                   dr.date,
                                   dr.nickname,
                                   dr.position AS position,
                                   np.name,
                                   np.dollar_value,
                                   np.fantasy_score
                                 FROM daily_roster dr
                                 JOIN nba_players np ON np.nba_player_uid = dr.nba_player_uid
                                 JOIN ranked_rosters rr ON dr.discord_player_id = rr.discord_player_id AND dr.guild_id = rr.guild_id
                                 WHERE dr.date = :date
                                   AND rr.rn = 1
                                 ORDER BY rr.total_score DESC, np.fantasy_score DESC
                                 LIMIT 100;
            """)
    List<DailyRosterPlayer> getTodaysGlobalRosters(LocalDate date);

    @Query(value = """
            SELECT dr.discord_player_id, dr.nickname, sum(np.fantasy_score) as fantasy_score
            FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
            WHERE dr.guild_id = :guildId AND dr.date < :endDay AND dr.date > :startDay
            GROUP BY dr.discord_player_id, dr.nickname
            ORDER BY fantasy_score DESC
            LIMIT 100
            """)
    List<DailyRosterPlayer> getWeeksLeaderboard(LocalDate startDay, LocalDate endDay, String guildId);

    @Query(value = """
            SELECT np.dollar_value FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
                    WHERE dr.guild_id = :guildId AND dr.discord_player_id = :discordId AND dr.date = :date
                                          AND dr.position <> :position::daily_roster_position
            """)
    List<Integer> getTodaysRosterPrice(String discordId, String guildId, String position, LocalDate date);

    @Query(value = """
            SELECT np.fantasy_score FROM nba_player np
            JOIN daily_roster dr on np.nba_player_uid = dr.nba_player_uid
            WHERE dr.guild_id = :guildId AND dr.discord_player_id = :discordId AND dr.date = CURRENT_DATE
            """)
    List<Double> getTodaysRosterFantasyScores(String discordId, String guildId);


    @Query(value = """
            DELETE FROM daily_roster dr
            WHERE dr.guild_id = :guildId
              AND dr.date = :date
              AND dr.discord_player_id = :discordId
              AND dr.nba_player_uid = :nbaPlayerUid
              )
            """)
    void deleteRosterPlayerByGuildIdAndDateAndDiscordIdAndPlayerName(String guildId, LocalDate date, String discordId, UUID nbaPlayerUid);


}
