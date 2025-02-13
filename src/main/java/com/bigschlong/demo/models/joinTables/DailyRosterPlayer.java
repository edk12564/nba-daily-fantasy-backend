package com.bigschlong.demo.models.joinTables;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Jacksonized
@Builder
public class DailyRosterPlayer {

    private UUID nbaPlayerUid;
    private Long nbaPlayerId;
    private String nickname;
    private String name;
    private Integer dollarValue;
    private Double fantasyScore;
    private String guildId;
    private String discordPlayerId;
    private Position position;
    private LocalDate date;

    // Spring Boot knows to add these to the SQL table
    public enum Position {
        PG, SG, SF, PF, C
    }

}
