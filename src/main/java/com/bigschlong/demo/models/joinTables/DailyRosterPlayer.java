package com.bigschlong.demo.models.joinTables;

import com.bigschlong.demo.models.dtos.DailyRoster;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Jacksonized
public class DailyRosterPlayer {

    @Id
    private DailyRosterId id;
    private UUID nbaPlayerUid;
    private String nickname;
    private String name;
    private Integer dollarValue;
    private Double fantasyScore;

    // Spring Boot knows to add these to the SQL table
    @Data
    @Jacksonized
    @Builder
    public static class DailyRosterId {
        private long guildId;
        private long discordPlayerId;
        private Position position;
        private LocalDate date;
    }

    public enum Position {
        PG, SG, SF, PF, C
    }

}
