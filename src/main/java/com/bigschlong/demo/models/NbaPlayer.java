package com.bigschlong.demo.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
//@Entity
@Table(name = "nba_players")
public class NbaPlayer {
    
    @Id
    private UUID nba_player_uid;
    
    private Integer nba_player_id;
    
    private String name;
    
    private String date;
    
    private String position;
    
    private Integer against_team;
    
    private Integer dollar_value;
    
    private Double fantasy_score;
    
    private Integer team_id;
    

//    @Id
//    @Column(name = "nba_player_uid", nullable = false)
//    private UUID id;
//
//    @Column(name = "nba_player_id", nullable = false)
//    private Integer nbaPlayerId;
//
//    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
//    private String name;
//
//    @Column(name = "date", nullable = false, length = Integer.MAX_VALUE)
//    private String date;
//
//    @Column(name = "position", nullable = false)
//    private String position;
//
//    @Column(name = "against_team")
//    private Integer againstTeam;
//
//    @Column(name = "dollar_value")
//    private Integer dollarValue;
//
//    @Column(name = "fantasy_score")
//    private Double fantasyScore;
    
//    // Join Column for Team entity
//    @OneToOne
//    @JoinColumn(name= "team_id", referencedColumnName="team_id")
//    private Team team;
    
    // Inverse Join Table for DiscordPlayer entity but with custom DailyRoster entity
//    This doesnt work for some reason.
//    @OneToMany(mappedBy = "nbaPlayerId")
//    private Set<DailyRoster> dailyRosterEntries;

}