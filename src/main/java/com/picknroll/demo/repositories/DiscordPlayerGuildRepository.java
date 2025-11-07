package com.picknroll.demo.repositories;

import com.picknroll.demo.models.dtos.DiscordPlayerGuild;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscordPlayerGuildRepository extends CrudRepository<DiscordPlayerGuild, String> {

    @Modifying
    @Query(value = """
                INSERT INTO discord_player_guilds (discord_player_id, guild_id)
                VALUES (:discordPlayerId, :guildId)
                ON CONFLICT (discord_player_id, guild_id) DO NOTHING
    """)
    void insertGuildForPlayerId(String discordPlayerId, String guildId);

}
