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
    void weightIsEqualForAllPlayers() {
        assertEquals(1, GachaEconomy.weight(null));
        assertEquals(1, GachaEconomy.weight(3));
        assertEquals(1, GachaEconomy.weight(36));
        assertEquals(GachaEconomy.WEIGHT_FLOOR, GachaEconomy.weight(99));
    }

    @Test
    void playerStarBands() {
        assertEquals(1, GachaEconomy.playerStar(14));
        assertEquals(2, GachaEconomy.playerStar(15));
        assertEquals(3, GachaEconomy.playerStar(24));
        assertEquals(4, GachaEconomy.playerStar(25));
        assertEquals(4, GachaEconomy.playerStar(28));
        assertEquals(5, GachaEconomy.playerStar(29));
        assertEquals(5, GachaEconomy.playerStar(34));
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

    @Test
    void salaryDiscountByRarity() {
        assertEquals(0, GachaEconomy.salaryDiscount((GachaRarity) null));
        assertEquals(0, GachaEconomy.salaryDiscount(GachaRarity.COMMON));
        assertEquals(1, GachaEconomy.salaryDiscount(GachaRarity.RARE));
        assertEquals(2, GachaEconomy.salaryDiscount(GachaRarity.EPIC));
        assertEquals(3, GachaEconomy.salaryDiscount(GachaRarity.LEGENDARY));
        assertEquals(3, GachaEconomy.salaryDiscount("LEGENDARY"));
        assertEquals(0, GachaEconomy.salaryDiscount("nope"));
    }

    @Test
    void billedSalaryFloorsAtOne() {
        assertEquals(28, GachaEconomy.billedSalary(31, 3));
        assertEquals(1, GachaEconomy.billedSalary(2, 3));
        assertEquals(0, GachaEconomy.billedSalary(null, 3));
        assertEquals(0, GachaEconomy.billedSalary(0, 1));
        assertEquals(30, GachaEconomy.billedSalary(30, "COMMON"));
    }
}
