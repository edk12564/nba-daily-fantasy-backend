package com.bigschlong.demo.models.joinTables;

import com.bigschlong.demo.models.dtos.DailyRoster;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Getter
@Setter
public class DailyRosterPlayer {

    @Id
    private DailyRoster.DailyRosterId id;
    @Id
    private UUID nbaPlayerUid;

    private String nickname;
    private String date;
    private String name;
    private String position;
    private Integer dollarValue;
    private Double fantasyScore;

}