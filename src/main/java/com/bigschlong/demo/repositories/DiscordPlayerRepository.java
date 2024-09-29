package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.DiscordPlayer;
import com.bigschlong.demo.models.NbaPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiscordPlayerRepository extends JpaRepository<DiscordPlayer, UUID> {
    
    @Query("""
    SELECT dr FROM DailyRoster dr
    JOIN dr.discordPlayerId dp
    JOIN dr.nbaPlayerId np
    WHERE dp.id = :discordId
    """)
    public List<NbaPlayer> getRosterByDiscordId(UUID discordId);
    
}
