package com.bigschlong.demo.models;

import com.bigschlong.demo.models.joinTables.DailyRoster;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "nba_players")
public class NbaPlayer {

    @Id
    @Column(name = "nba_player_uid", nullable = false)
    private UUID id;

    @Column(name = "nba_player_id", nullable = false)
    private Integer nbaPlayerId;

    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
    private String name;

    @Column(name = "date", nullable = false, length = Integer.MAX_VALUE)
    private String date;
    
    @Column(name = "against_team")
    private Integer againstTeam;

    @Column(name = "dollar_value")
    private Integer dollarValue;

    @Column(name = "fantasy_score")
    private Double fantasyScore;
    
    // Join Column for Team entity
    @OneToOne
    @JoinColumn(name= "team_id", referencedColumnName="team_id")
    private Team team;
    
    // Inverse Join Table for DiscordPlayer entity but with custom DailyRoster entity
    @OneToMany(mappedBy = "nbaPlayerId")
    private Set<DailyRoster> dailyRosterEntries;
    
}