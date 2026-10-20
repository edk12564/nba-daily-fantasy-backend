package com.picknroll.demo.repositories;

import com.picknroll.demo.models.dtos.GachaUserWallet;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GachaUserWalletRepository extends CrudRepository<GachaUserWallet, String> {

    @Query("""
            SELECT discord_player_id, rolls, dust, pity_counter, streak_days, streak_last_date, updated_at
            FROM gacha_user_wallet
            WHERE discord_player_id = :discordPlayerId
            """)
    Optional<GachaUserWallet> findWallet(String discordPlayerId);

    @Modifying
    @Transactional
    @Query("""
            INSERT INTO gacha_user_wallet (discord_player_id)
            VALUES (:discordPlayerId)
            ON CONFLICT (discord_player_id) DO NOTHING
            """)
    void ensureWallet(String discordPlayerId);

    @Modifying
    @Transactional
    @Query("""
            UPDATE gacha_user_wallet
            SET rolls = rolls + :amount, updated_at = now()
            WHERE discord_player_id = :discordPlayerId
            """)
    void addRolls(String discordPlayerId, int amount);

    @Modifying
    @Transactional
    @Query("""
            UPDATE gacha_user_wallet
            SET rolls = rolls - :amount, updated_at = now()
            WHERE discord_player_id = :discordPlayerId AND rolls >= :amount
            """)
    int tryDebitRolls(String discordPlayerId, int amount);

    @Modifying
    @Transactional
    @Query("""
            UPDATE gacha_user_wallet
            SET dust = dust + :amount, updated_at = now()
            WHERE discord_player_id = :discordPlayerId
            """)
    void addDust(String discordPlayerId, int amount);

    @Modifying
    @Transactional
    @Query("""
            UPDATE gacha_user_wallet
            SET dust = dust - :amount, updated_at = now()
            WHERE discord_player_id = :discordPlayerId AND dust >= :amount
            """)
    int trySubtractDust(String discordPlayerId, int amount);

    @Modifying
    @Transactional
    @Query("""
            UPDATE gacha_user_wallet
            SET pity_counter = :pityCounter, updated_at = now()
            WHERE discord_player_id = :discordPlayerId
            """)
    void setPityCounter(String discordPlayerId, int pityCounter);

    @Modifying
    @Transactional
    @Query("""
            UPDATE gacha_user_wallet
            SET streak_days = :streakDays, streak_last_date = :streakLastDate, updated_at = now()
            WHERE discord_player_id = :discordPlayerId
            """)
    void updateStreak(String discordPlayerId, int streakDays, LocalDate streakLastDate);
}
