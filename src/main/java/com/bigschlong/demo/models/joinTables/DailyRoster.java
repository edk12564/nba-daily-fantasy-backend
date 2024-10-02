package com.bigschlong.demo.models.joinTables;

import com.bigschlong.demo.models.compositeKeys.DailyRosterId;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Getter
@Setter
//@Entity
//@Table(name = "daily_roster")
public class DailyRoster {
    @Id
    private DailyRosterId id;
    
    private UUID discord_player_uid;
    
    private UUID nba_player_uid;
    
    
    
//    @EmbeddedId
//    private DailyRosterId dailyRosterId;
    
//    @ManyToOne
//    @MapsId("discordPlayerUid")
//    @JoinColumn(name = "discord_player_uid", nullable = false)
//    private DiscordPlayer discordPlayerId;
    
//    @ManyToOne
//    @MapsId("nbaPlayerUid")
//    @JoinColumn(name = "nba_player_uid", nullable = false)
//    private NbaPlayer nbaPlayerId;
}