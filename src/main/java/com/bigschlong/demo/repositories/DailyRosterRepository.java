package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.dtos.DailyRoster;
import com.bigschlong.demo.models.dtos.IsLocked;
import com.bigschlong.demo.models.joinTables.DailyRosterPlayer;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface DailyRosterRepository extends CrudRepository<DailyRoster, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO daily_roster (discord_player_id, nba_player_uid, guild_id, date, nickname, position)
                            VALUES (:discordPlayerId, :nbaPlayerUid, :guildId, CURRENT_DATE, :nickname, :position::daily_roster_position)
                            ON CONFLICT(discord_player_id, guild_id, date, position) DO UPDATE
                            SET nba_player_uid = :nbaPlayerUid
            """)
    void saveRosterChoice(UUID nbaPlayerUid, String discordPlayerId, String guildId, String nickname, String position);

    @Query(value = """
            SELECT dr.*, np.name, np.position, np.dollar_value FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
            WHERE dr.discord_player_id = :discordId AND dr.guild_id = :guildId
            """)
    List<DailyRosterPlayer> getRosterByDiscordIdAndGuildId(String discordId, String guildId);

    @Query(value = """
            SELECT dr.*, np.name, np.position, np.dollar_value FROM daily_roster dr
            JOIN nba_players np on np.nba_player_uid = dr.nba_player_uid
            WHERE dr.guild_id = :guildId
    ORDER BY dr.nickname
            """)
    List<DailyRosterPlayer> getRostersByGuildId(String guildId);


}
