package com.picknroll.demo.models.dtos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
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

}