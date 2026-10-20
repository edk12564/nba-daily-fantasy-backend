package com.picknroll.demo.models.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@Builder
@Jacksonized
@Table("gacha_skins")
public class GachaSkin {

    @Id
    private UUID skinId;
    private Long nbaPlayerId;
    private String rarity;
    private String artUrl;
    private String frameKey;
    private String seasonLabel;
}
