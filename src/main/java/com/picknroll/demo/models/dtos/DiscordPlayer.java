package com.picknroll.demo.models.dtos;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@Table(name = "discord_players")
public class DiscordPlayer {
    @Id
    private String discord_player_uid;
    
    private String name;


//    @Id
//    @Column(name = "discord_player_uid", nullable = false)
//    private UUID id;

//    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
//    private String name;
    
    // Join Table for NbaPlayer entity using the DailyRoster entity as the join table
    // This might be a problem because Hibernate might automatically fetch this while run when joining and create a fetch loop with DailyRoster object.
//    @OneToMany(mappedBy = "discordPlayerId", fetch = FetchType.LAZY)
//    private Set<DailyRoster> dailyRosterEntries;
//
//    @OneToMany(mappedBy = "discordPlayerId", fetch = FetchType.LAZY)
//    private Set<DiscordPlayerServer> discordPlayerServer;
}