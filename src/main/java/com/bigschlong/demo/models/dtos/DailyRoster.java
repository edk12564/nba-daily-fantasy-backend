package com.bigschlong.demo.models.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@Jacksonized
@Table(name = "daily_roster")
public class DailyRoster {
    @Id
    private DailyRosterId id;
    private String nickname;
    private LocalDate date;


    // Spring Boot knows to add these to the SQL table
    @Data
    @Jacksonized
    @Builder
    public static class DailyRosterId {
         long guild_id;

         long nba_player_uid;

         long discord_player_id;

        String position;
    }
}
    
    

