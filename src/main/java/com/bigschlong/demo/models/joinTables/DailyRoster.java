package com.bigschlong.demo.models.joinTables;

import com.bigschlong.demo.models.DiscordPlayer;
import com.bigschlong.demo.models.NbaPlayer;
import com.bigschlong.demo.models.compositeKeys.DailyRosterId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "daily_roster")
public class DailyRoster {
    @EmbeddedId
    private DailyRosterId dailyRosterId;
    
    @ManyToOne
    @MapsId("discordPlayerUid")
    @JoinColumn(name = "discord_player_uid", nullable = false)
    private DiscordPlayer discordPlayerId;
    
    @ManyToOne
    @MapsId("nbaPlayerUid")
    @JoinColumn(name = "nba_player_uid", nullable = false)
    private NbaPlayer nbaPlayerId;
}