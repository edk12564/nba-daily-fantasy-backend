package com.picknroll.demo.repositories;

import com.picknroll.demo.models.dtos.GachaUserSkin;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface GachaUserSkinRepository extends CrudRepository<GachaUserSkin, String> {

    @Query("""
            SELECT COUNT(*) FROM gacha_user_skins
            WHERE discord_player_id = :discordPlayerId AND skin_id = :skinId
            """)
    int countOwnership(String discordPlayerId, UUID skinId);

    @Modifying
    @Transactional
    @Query("""
            INSERT INTO gacha_user_skins (discord_player_id, skin_id)
            VALUES (:discordPlayerId, :skinId)
            ON CONFLICT (discord_player_id, skin_id) DO NOTHING
            """)
    int insertOwnership(String discordPlayerId, UUID skinId);

    @Modifying
    @Transactional
    @Query("""
            DELETE FROM gacha_user_skins
            WHERE discord_player_id = :discordPlayerId AND skin_id = :skinId
            """)
    void deleteOwnership(String discordPlayerId, UUID skinId);
}
