package com.picknroll.demo.repositories;

import com.picknroll.demo.models.dtos.DailyRoster;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface DailyRosterRepository extends CrudRepository<DailyRoster, UUID> {

    /* Save Roster */
    @Modifying
    @Query(value = """
            INSERT INTO daily_roster (discord_player_id, nba_player_uid, date, nickname, position)
                            VALUES (:discordPlayerId, :nbaPlayerUid, :date, :nickname, :position::daily_roster_position)
                            ON CONFLICT(discord_player_id, date, position) DO UPDATE
                            SET nba_player_uid = :nbaPlayerUid, position = :position::daily_roster_position
            """)
    void saveRosterChoice(UUID nbaPlayerUid, String discordPlayerId, String nickname, String position, LocalDate date);

    // Race condition logic here?


    @Query(value = """
                    SELECT dr.*, np.nba_player_uid, np.nba_player_id, np.name, np.dollar_value FROM daily_roster dr
                    JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
                    WHERE dr.discord_player_id = :discordId AND dr.date = :date
            """)
    List<DailyRosterPlayer> getTodaysRosterByDiscordId(String discordId, LocalDate date);

    /* Leaderboard */
//    This might need modification. this is getting the global leaderboard. but that will already be in the dailyrosters table now. this is now the default, meaning this long sql query probably isn't needed.
    @Query(value = """
            WITH roster_totals AS (
                                   SELECT
                                     dr.discord_player_id,
                                     SUM(np.fantasy_score) AS total_score
                                   FROM daily_roster dr
                                   JOIN nba_players np ON np.nba_player_uid = dr.nba_player_uid
                                   WHERE dr.date = :date
                                   GROUP BY dr.discord_player_id
                                 ),
                                 ranked_rosters AS (
                                   SELECT
                                     discord_player_id,
                                     total_score,
                                     ROW_NUMBER() OVER (PARTITION BY discord_player_id ORDER BY total_score DESC) AS rn
                                   FROM roster_totals
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
                                   np.fantasy_score
                                 FROM daily_roster dr
                                 JOIN nba_players np ON np.nba_player_uid = dr.nba_player_uid
                                 JOIN ranked_rosters rr ON dr.discord_player_id = rr.discord_player_id
                                 WHERE dr.date = :date
                                   AND rr.rn = 1
                                 ORDER BY rr.total_score DESC, np.fantasy_score DESC
                                 LIMIT 100;
            """)
    List<DailyRosterPlayer> getTodaysGlobalLeaderboard(LocalDate date);

//    this should also go by server. like 2 queries above, we need to join and search by guild_id.
    @Query(value = """
            SELECT dr.discord_player_id, dr.nickname, sum(np.fantasy_score) as fantasy_score
            FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
            WHERE dr.date < :endDay AND dr.date > :startDay
            GROUP BY dr.discord_player_id, dr.nickname
            ORDER BY fantasy_score DESC
            LIMIT 100
            """)
    List<DailyRosterPlayer> getWeeksLeaderboardByGuildId(String guildId, LocalDate startDay, LocalDate endDay);

    //    This has to be modified to use joins to check by the discord player id. this is because this is the guild specific leaderboard.
    @Query(value = """
            SELECT dr.discord_player_id, np.nba_player_id, dr.nba_player_uid, dr.date, dr.nickname, dr.position AS position, np.name, np.dollar_value, np.fantasy_score 
            FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
            JOIN discord_player_guilds dpg on dpg.nba_player_id = dr.nba_player_id
            WHERE dr.date = :date and dpg.guild_id = :guildId
            ORDER BY np.fantasy_score DESC
            LIMIT 1000
            """)
    List<DailyRosterPlayer> getTodaysLeaderboardByGuildId(String guildId, LocalDate date);

    /*  */
    @Query(value = """
            SELECT np.dollar_value FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
                    WHERE dr.discord_player_id = :discordId AND dr.date = :date
                                          AND dr.position <> :position::daily_roster_position
            """)
    List<Integer> getTodaysRosterPrice(String discordId, String position, LocalDate date);

    @Query(value = """
            DELETE FROM daily_roster dr
            WHERE dr.date = :date
              AND dr.discord_player_id = :discordId
              AND dr.nba_player_uid = :nbaPlayerUid
              )
            """)
    void deleteRosterPlayerByDateAndDiscordIdAndPlayerName(LocalDate date, String discordId, UUID nbaPlayerUid);




}
