package com.bigschlong.demo.models.joinTables;

import com.bigschlong.demo.models.dtos.DailyRoster;
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
    private DailyRoster.DailyRosterId id;
    private UUID nbaPlayerUid;
    private String nickname;
    private String name;
    private Integer dollarValue;
    private Double fantasyScore;

}
