package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.dtos.DiscordPlayer;
import com.bigschlong.demo.models.dtos.NbaPlayer;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiscordPlayerRepository extends CrudRepository<DiscordPlayer, UUID> {
    
    @Query(value = """
    SELECT np.* FROM daily_roster dr
    JOIN discord_players dp on dp.discord_player_uid = dr.discord_player_uid
    JOIN  nba_players np on np.nba_player_uid = dr.nba_player_uid
    WHERE dp.discord_player_uid = :discordId
    """)
    List<NbaPlayer> getRosterByDiscordId( UUID discordId);
    
    
//    @Query("""
//    SELECT np
//        FROM DailyRoster dr
//        JOIN dr.discordPlayerId dp
//        JOIN dr.nbaPlayerUid np
//        WHERE dp.id = :discordId
//        """)
//      List<NbaPlayer> getRosterByDiscordId(UUID discordId);

}
