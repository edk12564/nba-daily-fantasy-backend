package com.bigschlong.demo.models.joinTables;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DailyRosterPlayer {
    private UUID nbaPlayerId;
    private UUID discordPlayerUid;
    private String name;
    private Integer dollarValue;

}