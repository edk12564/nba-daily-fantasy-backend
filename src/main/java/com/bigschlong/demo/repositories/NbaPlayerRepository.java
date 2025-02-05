package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.dtos.NbaPlayer;
import com.bigschlong.demo.models.dtos.Team;
import com.bigschlong.demo.models.joinTables.NbaPlayerTeam;
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
    List<Team> getTeamByPlayerUid(UUID playerId);

    @Query(value = """
    SELECT * FROM nba_players
    WHERE nba_player_uid = :nbaPlayerUid;
    """)
    NbaPlayer findNbaPlayerByUid(UUID nbaPlayerUid);

    @Query(value = """
    SELECT * FROM nba_players
    WHERE name = :name;
    """)
    NbaPlayer findNbaPlayerByName(String name);

    @Query(value = """
    SELECT np.* FROM nba_players np
    WHERE np.position = :position
    ORDER BY np.dollar_value DESC
    LIMIT 25
    """)
    List<NbaPlayer> getTodaysNbaPlayersByPosition(String position);

    @Query(value = """
    SELECT np.* FROM nba_players np
    ORDER BY np.dollar_value DESC
    LIMIT 25
    """)
    List<NbaPlayer> getAllTodaysNbaPlayers();

    @Query(value = """
            SELECT np.name AS player_name, t.name AS team_name, at.name as against_team_name, np.*
    FROM nba_players np
    JOIN teams t on t.team_id = np.team_id
            JOIN teams at on np.against_team = at.team_id
    ORDER BY np.dollar_value DESC
    LIMIT 25
    """)
    List<NbaPlayerTeam> getNbaPlayersWithTeam();

}


