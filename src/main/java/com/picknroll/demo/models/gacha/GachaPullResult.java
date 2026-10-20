package com.picknroll.demo.models.gacha;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class GachaPullResult {
    private String outcome; // NEW | DUPLICATE
    private String rarity;
    private Long nbaPlayerId;
    private UUID skinId;
    private String artUrl;
    private String frameKey;
    private int dustGranted;
    private int playerStar;
}
