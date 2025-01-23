package com.bigschlong.demo.models.joinTables;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
//@Entity
@Builder
@Jacksonized
@Table(name = "daily_roster")
public class DailyRoster {
    @Id
    private DailyRosterId id;

    String nickname;

    // discordplayerserver
    // playerId
    // guildId
    // server nickname
    // (guildId, nbaPlayerUid, discordPlayerId), nickname
    //    ^composite primary key


    // Spring Boot knows to add these to the SQL table
    @Data
    @Jacksonized
    @Builder
    public static class DailyRosterId {
         long guildId;

         long nbaPlayerUid;

         long discordPlayerId;
    }
}
    
    

