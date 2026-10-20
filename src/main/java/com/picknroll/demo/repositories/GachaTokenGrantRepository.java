package com.picknroll.demo.repositories;

import com.picknroll.demo.models.dtos.GachaTokenGrant;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface GachaTokenGrantRepository extends CrudRepository<GachaTokenGrant, UUID> {

    @Modifying
    @Transactional
    @Query("""
            INSERT INTO gacha_token_grants (discord_player_id, reason, period_key, amount)
            VALUES (:discordPlayerId, :reason, :periodKey, :amount)
            ON CONFLICT (discord_player_id, reason, period_key) DO NOTHING
            """)
    int tryInsertGrant(String discordPlayerId, String reason, String periodKey, int amount);
}
