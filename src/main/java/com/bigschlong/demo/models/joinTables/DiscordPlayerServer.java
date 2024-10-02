package com.bigschlong.demo.models.joinTables;

import com.bigschlong.demo.models.compositeKeys.DiscordPlayerServerId;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Getter
@Setter
//@Entity
//@Table(name = "discord_player_servers")
public class DiscordPlayerServer {
    @Id
    private DiscordPlayerServerId id;
    
    private UUID discord_player_uid;
    
    private UUID server_uid;
    
//    @EmbeddedId
//    private DiscordPlayerServerId discordPlayerServerId;
    
//    @ManyToOne
//    @MapsId("discordPlayerUid")
//    @JoinColumn(name = "discord_player_uid", nullable = false)
//    private DiscordPlayer discordPlayerId;
//
//    @ManyToOne
//    @MapsId("serverUid")
//    @JoinColumn(name = "server_uid", nullable = false)
//    private Server serverId;
}