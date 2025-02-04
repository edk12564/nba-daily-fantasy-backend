package com.bigschlong.demo.models.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Data
@Jacksonized
@Builder
public class SetPlayerDTO {
    UUID nba_player_uid;
    String position;
}
