package com.picknroll.demo.repositories;

import com.picknroll.demo.models.dtos.DiscordPlayerGuild;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscordPlayerGuildRepository extends CrudRepository<DiscordPlayerGuild, String> {

    @Query(value = """
    SELECT discord_player_id FROM discord_player_guilds dpg
    WHERE guild_id = :guildId
    """)
    List<String> getDiscordPlayersByGuildId(String guildId);

}
