package com.picknroll.demo.repositories;

import com.picknroll.demo.models.dtos.DiscordChannel;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface DiscordChannelRepository extends CrudRepository<DiscordChannel, DiscordChannel.DiscordChannelId> {

    @Query("""
            INSERT INTO discord_channels(channel_id, date, created_at, guild_id)
            VALUES (:channelId, :date, NOW(), :guildId)
            ON CONFLICT (channel_id, date)
            DO UPDATE SET
            created_at = NOW()
            """)
    @Modifying
    @Transactional
    void insertChannelForDate(String channelId, LocalDate date, String guildId);
}
