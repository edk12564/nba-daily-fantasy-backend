package com.bigschlong.demo.models.joinTables;

import java.util.UUID;

public class DailyRosterPlayer {
    private UUID nbaPlayerId;
    private UUID discordPlayerUid;
    private String name;
    private double dollarValue;

    // Getters and setters
    public UUID getNbaPlayerId() {
        return nbaPlayerId;
    }

    public void setNbaPlayerId(UUID nbaPlayerId) {
        this.nbaPlayerId = nbaPlayerId;
    }

    public UUID getDiscordPlayerUid() {
        return discordPlayerUid;
    }

    public void setDiscordPlayerUid(UUID discordPlayerUid) {
        this.discordPlayerUid = discordPlayerUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getDollarValue() {
        return dollarValue;
    }

    public void setDollarValue(double dollarValue) {
        this.dollarValue = dollarValue;
    }
}