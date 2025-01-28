package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.dtos.DiscordPlayer;
import com.bigschlong.demo.models.dtos.Server;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServerRepository extends CrudRepository<Server, UUID> {
    
    @Query(value = """
    SELECT dp.* FROM discord_player_servers dps
    JOIN servers s on s.server_uid = dps.server_uid
    JOIN discord_players dp on dp.player_uid = dps.player_uid
    WHERE s.server_uid = :serverId
    """)
    List<DiscordPlayer> getDiscordPlayersByServerId(UUID serverId);

}
