package com.picknroll.demo.repositories;

import com.picknroll.demo.models.dtos.GachaSkin;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GachaSkinRepository extends CrudRepository<GachaSkin, UUID> {

    @Query("""
            SELECT skin_id, nba_player_id, rarity, art_url, frame_key, season_label
            FROM gacha_skins
            WHERE nba_player_id = :nbaPlayerId AND rarity = :rarity
            """)
    Optional<GachaSkin> findByNbaPlayerIdAndRarity(long nbaPlayerId, String rarity);

    @Query("""
            SELECT gs.skin_id, gs.nba_player_id, gs.rarity, gs.art_url, gs.frame_key, gs.season_label
            FROM gacha_user_skins gus
            JOIN gacha_skins gs ON gs.skin_id = gus.skin_id
            WHERE gus.discord_player_id = :discordPlayerId
            ORDER BY
              CASE gs.rarity
                WHEN 'LEGENDARY' THEN 4
                WHEN 'EPIC' THEN 3
                WHEN 'RARE' THEN 2
                ELSE 1
              END DESC,
              gs.nba_player_id
            """)
    List<GachaSkin> findOwnedSkins(String discordPlayerId);
}
