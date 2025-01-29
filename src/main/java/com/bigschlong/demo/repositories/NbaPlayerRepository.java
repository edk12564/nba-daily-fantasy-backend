package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.dtos.NbaPlayer;
import com.bigschlong.demo.models.dtos.Team;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NbaPlayerRepository extends CrudRepository<NbaPlayer, UUID> {
    
    @Query(value = """
    SELECT t.* FROM nba_players np
    JOIN teams t on t.team_id = np.team_id
    WHERE np.nba_player_uid = :playerId
    """)
    List<Team> getTeamByPlayerId(UUID playerId);

    @Query(value = """
    SELECT np.* FROM nba_players np
    WHERE np.position = :position
    ORDER BY np.dollar_value DESC
    LIMIT 1
    """)
    List<NbaPlayer> getTodaysNbaPlayersByPosition(String position);

    @Query(value = """
    SELECT np.* FROM nba_players np
    ORDER BY np.dollar_value DESC
    LIMIT 1
    """)
    List<NbaPlayer> getAllTodaysNbaPlayers();

}


