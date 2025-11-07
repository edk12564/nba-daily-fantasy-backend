package com.picknroll.demo.models.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Jacksonized
@Table(name = "discord_player_guilds")
public class DiscordPlayerGuild {

    @Id
    private String discordPlayerId;
    private String guildId;

}