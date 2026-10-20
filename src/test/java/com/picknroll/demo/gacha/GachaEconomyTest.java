package com.picknroll.demo.gacha;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GachaEconomyTest {

    @Test
    void baseRatesSumToOne() {
        double sum = GachaEconomy.baseRates().values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 1e-9);
    }

    @Test
    void softPityIncreasesLegendaryFromCommon() {
        Map<GachaRarity, Double> base = GachaEconomy.ratesForPity(1);
        Map<GachaRarity, Double> soft = GachaEconomy.ratesForPity(41);
        assertTrue(soft.get(GachaRarity.LEGENDARY) > base.get(GachaRarity.LEGENDARY));
        assertTrue(soft.get(GachaRarity.COMMON) < base.get(GachaRarity.COMMON));
        assertEquals(0.015 + 0.005, soft.get(GachaRarity.LEGENDARY), 1e-9);
    }

    @Test
    void hardPityGuaranteesLegendary() {
        Map<GachaRarity, Double> hard = GachaEconomy.ratesForPity(80);
        assertEquals(1.0, hard.get(GachaRarity.LEGENDARY), 1e-9);
        assertEquals(0.0, hard.get(GachaRarity.COMMON), 1e-9);
    }

    @Test
    void weightUsesFloor() {
        assertEquals(8, GachaEconomy.weight(null));
        assertEquals(8, GachaEconomy.weight(3));
        assertEquals(36, GachaEconomy.weight(36));
    }

    @Test
    void playerStarBands() {
        assertEquals(1, GachaEconomy.playerStar(12));
        assertEquals(2, GachaEconomy.playerStar(13));
        assertEquals(3, GachaEconomy.playerStar(25));
        assertEquals(4, GachaEconomy.playerStar(26));
        assertEquals(5, GachaEconomy.playerStar(36));
    }

    @Test
    void rarePlusRatesExcludeCommon() {
        Map<GachaRarity, Double> rates = GachaEconomy.rarePlusRates();
        assertEquals(0.0, rates.get(GachaRarity.COMMON), 1e-9);
        double sum = rates.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 1e-9);
    }

    @Test
    void podiumAmountsByRank() {
        assertEquals(80, GachaEconomy.dailyPodiumAmount(1));
        assertEquals(50, GachaEconomy.dailyPodiumAmount(2));
        assertEquals(30, GachaEconomy.dailyPodiumAmount(3));
        assertEquals(0, GachaEconomy.dailyPodiumAmount(4));
        assertEquals(150, GachaEconomy.weeklyPodiumAmount(1));
        assertEquals(100, GachaEconomy.weeklyPodiumAmount(2));
        assertEquals(60, GachaEconomy.weeklyPodiumAmount(3));
        assertEquals(0, GachaEconomy.weeklyPodiumAmount(0));
    }
}
