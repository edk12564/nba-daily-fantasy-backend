package com.bigschlong.demo.models;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
//@Entity
//@Table(name = "discord_players")
public class DiscordPlayer {
    @Id
    private UUID discord_player_uid;
    
    private String name;
    
//    @Id
//    @Column(name = "discord_player_uid", nullable = false)
//    private UUID id;

//    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
//    private String name;
    
    // Join Table for NbaPlayer entity using the DailyRoster entity as the join table
    @OneToMany(mappedBy = "discordPlayerId")
    private Set<DailyRoster> dailyRosterEntries;
    
    @OneToMany(mappedBy = "discordPlayerId")
    private Set<DiscordPlayerServer> discordPlayerServer;
}