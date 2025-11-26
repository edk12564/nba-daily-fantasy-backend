package com.picknroll.demo.models.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Jacksonized
@Builder
public class SetPlayerDTO {

    UUID nba_player_uid;
    // TODO: get rid of position
    String position;
    String nickname;
    String discord_player_id;
    // TODO: date is no longer used at all
    LocalDate date;
    String channel_id;
}
