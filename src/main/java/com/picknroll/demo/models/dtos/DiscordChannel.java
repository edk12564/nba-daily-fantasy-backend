package com.picknroll.demo.models.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Jacksonized
public class DiscordChannel {
    @Id
    DiscordChannelId id;

    LocalDateTime createdAt;
    String guildId;

    @Getter
    @Setter
    @Builder
    @Jacksonized
    public static class DiscordChannelId {
        String channelId;
        LocalDate date;

    }
}
