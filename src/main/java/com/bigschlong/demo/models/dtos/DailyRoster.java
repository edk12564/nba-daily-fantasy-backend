package com.bigschlong.demo.models.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@Jacksonized
@Table(name = "daily_roster")
public class DailyRoster {
    @Id
    private DailyRosterId id;
    private String nickname;
    private UUID nbaPlayerUid;


    // Spring Boot knows to add these to the SQL table
    @Data
    @Jacksonized
    @Builder
    public static class DailyRosterId {
         long guildId;
         long discordPlayerId;
         String position;
         LocalDate date;
    }
}
    
    

