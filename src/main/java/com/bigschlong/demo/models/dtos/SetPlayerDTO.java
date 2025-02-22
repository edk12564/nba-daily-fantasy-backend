package com.bigschlong.demo.models.dtos;

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
    String position;
    String guild_id;
    String nickname;
    String discord_player_id;
    LocalDate date;
}
