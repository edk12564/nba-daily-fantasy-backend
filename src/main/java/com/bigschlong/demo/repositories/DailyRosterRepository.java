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
                            VALUES (:discordPlayerId, :nbaPlayerUid, :guildId, CURRENT_DATE, :nickname, :position::daily_roster_position)
                            ON CONFLICT(discord_player_id, guild_id, date, position) DO UPDATE
                            SET nba_player_uid = :nbaPlayerUid, position = :position::daily_roster_position
            """)
    void saveRosterChoice(UUID nbaPlayerUid, String discordPlayerId, String guildId, String nickname, String position);

    @Query(value = """
            SELECT dr.*, np.nba_player_uid, np.nba_player_id, np.name, np.dollar_value FROM daily_roster dr
    JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
            WHERE dr.discord_player_id = :discordId AND dr.guild_id = :guildId AND dr.date = :date
    """)
    List<DailyRosterPlayer> getTodaysRosterByDiscordIdAndGuildId(String discordId, String guildId, LocalDate date);

    // Changed these queries to not use *. Lets see if that makes a difference.
    @Query(value = """
    SELECT dr.discord_player_id, dr.nba_player_uid, dr.guild_id, dr.date, dr.nickname, dr.position AS position, np.name, np.dollar_value
    FROM daily_roster dr
    JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
    WHERE dr.guild_id = :guildId AND dr.date = CURRENT_DATE
    ORDER BY dr.nickname
    """)
    List<DailyRosterPlayer> getTodaysRostersByGuildId(String guildId);

    @Query(value = """
    SELECT dr.discord_player_id, dr.nba_player_uid, dr.guild_id, dr.date, dr.nickname, dr.position AS position, np.name, np.dollar_value, np.fantasy_score 
    FROM daily_roster dr
    JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
    WHERE dr.guild_id = :guildId AND dr.date = CURRENT_DATE
    ORDER BY dr.nickname
    """)
    List<DailyRosterPlayer> getTodaysRostersByGuildIdWithFantasyScore(String guildId);

    @Query(value = """
    SELECT dr.*, np.name, np.dollar_value FROM daily_roster dr
    JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
    WHERE dr.discord_player_id = :discordId AND dr.guild_id = :guildId AND dr.date = CURRENT_DATE AND np.position = :position
    """)
    List<DailyRosterPlayer> getTodaysRosterByPosition(String discordId, String guildId, String position);

    @Query(value = """
    SELECT np.dollar_value FROM daily_roster dr
    JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
            WHERE dr.guild_id = :guildId AND dr.discord_player_id = :discordId AND dr.date = :date
    """)
    List<Integer> getTodaysRosterPrice(String discordId, String guildId, LocalDate date);

    @Query(value = """
    SELECT np.fantasy_score FROM nba_player np
    JOIN daily_roster dr on np.nba_player_uid = dr.nba_player_uid
    WHERE dr.guild_id = :guildId AND dr.discord_player_id = :discordId AND dr.date = CURRENT_DATE
    """)
    List<Double> getTodaysRosterFantasyScores(String discordId, String guildId);


}
