package com.picknroll.demo.models.joinTables;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class NbaPlayerTeam {

    UUID nba_player_uid;
    Integer nba_player_id;
    String player_name;
    String date;
    String position;
    Integer against_team;
    Integer dollar_value;
    Double fantasy_score;
    Integer team_id;
    String team_name;
    String against_team_name;
    String status;
    String jersey_num;

    String avg_stl;
    String avg_tov;
    String avg_reb;
    String avg_pts;
    String avg_ast;
    String avg_blk;

}