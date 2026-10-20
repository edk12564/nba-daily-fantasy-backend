package com.picknroll.demo.models.joinTables;

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

    /** Owner's highest owned gacha rarity for this nba_player_id (null if none). */
    private String equippedSkinRarity;
    private String equippedArtUrl;
    private String frameKey;
    private Integer playerStar;

    // Spring Boot knows to add these to the SQL table
    public enum Position {
        PG, SG, SF, PF, C
    }

}
