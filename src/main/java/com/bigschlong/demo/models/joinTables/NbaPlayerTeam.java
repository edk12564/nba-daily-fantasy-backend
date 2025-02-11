package com.bigschlong.demo.models.joinTables;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class NbaPlayerTeam {

    private UUID nba_player_uid;

    private Integer nba_player_id;
    private String player_name;
    private String date;
    private String position;
    private Integer against_team;
    private Integer dollar_value;
    private Double fantasy_score;
    private Integer team_id;
    private String team_name;
    private String against_team_name;

}