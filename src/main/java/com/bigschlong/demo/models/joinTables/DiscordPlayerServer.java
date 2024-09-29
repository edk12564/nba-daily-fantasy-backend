package com.bigschlong.demo.models.joinTables;

import com.bigschlong.demo.models.DiscordPlayer;
import com.bigschlong.demo.models.Server;
import com.bigschlong.demo.models.compositeKeys.DiscordPlayerServerId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "discord_player_servers")
public class DiscordPlayerServer {
    @EmbeddedId
    private DiscordPlayerServerId discordPlayerServerId;
    
    @ManyToOne
    @MapsId("discordPlayerUid")
    @JoinColumn(name = "discord_player_uid", nullable = false)
    private DiscordPlayer discordPlayerId;
    
    @ManyToOne
    @MapsId("serverUid")
    @JoinColumn(name = "server_uid", nullable = false)
    private Server serverId;
}