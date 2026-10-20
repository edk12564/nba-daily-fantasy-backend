package com.picknroll.demo.models.gacha;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class GachaInventoryItem {
    private UUID skinId;
    private Long nbaPlayerId;
    private String playerName;
    private String rarity;
    private String artUrl;
    private String frameKey;
    private boolean displaySkin;
    private Integer playerStar;
}
