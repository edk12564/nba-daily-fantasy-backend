package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.NbaPlayer;
import com.bigschlong.demo.models.Team;
import org.springframework.data.jpa.repository.Query;
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
    """, nativeQuery = true)
    List<Team> getTeamByPlayerId(UUID playerId);

}


