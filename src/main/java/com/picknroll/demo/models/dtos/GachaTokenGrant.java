package com.picknroll.demo.models.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Jacksonized
@Table("gacha_token_grants")
public class GachaTokenGrant {

    @Id
    private UUID id;
    private String discordPlayerId;
    private String reason;
    private String periodKey;
    private int amount;
    private OffsetDateTime createdAt;
}
