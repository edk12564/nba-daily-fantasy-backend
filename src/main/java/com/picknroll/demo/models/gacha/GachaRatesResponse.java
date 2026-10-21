package com.picknroll.demo.models.gacha;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class GachaRatesResponse {
    private int pullCost;
    private Map<String, Double> rarityRates;
    private Map<String, Integer> dustOnDuplicate;
    private int softPityStart;
    private double softPityLegendaryBonusPerPull;
    private int hardPityAt;
    private int weightFloor;
    /** Roster salary discount by rarity name (COMMON=0, RARE=1, EPIC=2, LEGENDARY=3). */
    private Map<String, Integer> salaryDiscountByRarity;
}
