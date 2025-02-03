package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.dtos.DailyRoster;
import com.bigschlong.demo.models.dtos.NbaPlayer;
import com.bigschlong.demo.models.joinTables.DailyRosterPlayer;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface DailyRosterRepository extends CrudRepository<DailyRoster, UUID> {

    @Query(value = """
    INSERT INTO daily_roster (discord_player_id, nba_player_id, guild_id, date, nickname)
    VALUES (:discordPlayerId, :nbaPlayerId, :guildId, CURRENT_TIMESTAMP, :nickname)
    ON CONFLICT (nickname, date) DO UPDATE
    SET nickname = EXCLUDED.nickname, created_at = CURRENT_TIMESTAMP;
    """)
    void saveRosterChoice(UUID nbaPlayerId, String discordPlayerId, String guildId, String nickname);

    // check to make sure this is the right join with uid and uid
    @Query(value = """
    SELECT dr.*, np.name, np.position, np.dollar_value FROM daily_roster dr
    JOIN nba_players np on np.nba_player_uid = dr.nba_player_id
    WHERE dr.discord_player_id = :discordId
    """)
    List<DailyRosterPlayer> getRosterByDiscordId(String discordId);

    @Query(value = """
    SELECT dr.*, np.name, np.position, np.dollar_value FROM daily_roster dr
    JOIN nba_players np on np.nba_player_uid = dr.nba_player_id
    WHERE dr.guild_id = :guildId
    """)
    List<DailyRosterPlayer> getRosterByGuildId(String guildId);

}
