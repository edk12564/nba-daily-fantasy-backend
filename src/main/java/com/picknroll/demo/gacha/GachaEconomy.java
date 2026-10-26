package com.picknroll.demo.gacha;

import java.util.EnumMap;
import java.util.Map;

public final class GachaEconomy {

    public static final int PULL_COST = 100;
    /** Uniform pull weight — every player in today's pool is equally likely. */
    public static final int WEIGHT_FLOOR = 1;
    public static final int SOFT_PITY_START = 40;
    public static final int HARD_PITY_AT = 80;
    public static final double SOFT_PITY_LEGENDARY_BONUS = 0.005;

    /** Rolls granted once when a wallet is first created (10 pulls at PULL_COST). */
    public static final int STARTER_ROLLS = 1000;

    public static final int ROSTER_COMPLETE_ROLLS = 40;
    public static final int STREAK_3_ROLLS = 50;
    public static final int STREAK_7_ROLLS = 80;
    public static final int STREAK_14_ROLLS = 200;

    /**
     * Guild podium amounts. Ranked on each guild board, but paid at most once per
     * period using the player's best rank across guilds (no multi-guild stacking).
     */
    public static final int GUILD_DAILY_1ST = 80;
    public static final int GUILD_DAILY_2ND = 50;
    public static final int GUILD_DAILY_3RD = 30;
    public static final int GUILD_WEEKLY_1ST = 150;
    public static final int GUILD_WEEKLY_2ND = 100;
    public static final int GUILD_WEEKLY_3RD = 60;

    public static final String REASON_STARTER = "STARTER";
    /** period_key for the one-time starter credit (unique with reason). */
    public static final String PERIOD_STARTER = "once";
    public static final String REASON_ROSTER_COMPLETE = "ROSTER_COMPLETE";
    public static final String REASON_STREAK_3 = "STREAK_3";
    public static final String REASON_STREAK_7 = "STREAK_7";
    public static final String REASON_STREAK_14 = "STREAK_14";
    public static final String REASON_GUILD_DAILY_PODIUM = "GUILD_DAILY_PODIUM";
    public static final String REASON_GUILD_WEEKLY_PODIUM = "GUILD_WEEKLY_PODIUM";

    private static final Map<GachaRarity, Double> BASE_RATES = new EnumMap<>(GachaRarity.class);
    private static final Map<GachaRarity, Integer> DUST_ON_DUPE = new EnumMap<>(GachaRarity.class);

    static {
        BASE_RATES.put(GachaRarity.COMMON, 0.70);
        BASE_RATES.put(GachaRarity.RARE, 0.22);
        BASE_RATES.put(GachaRarity.EPIC, 0.065);
        BASE_RATES.put(GachaRarity.LEGENDARY, 0.015);

        DUST_ON_DUPE.put(GachaRarity.COMMON, 5);
        DUST_ON_DUPE.put(GachaRarity.RARE, 20);
        DUST_ON_DUPE.put(GachaRarity.EPIC, 60);
        DUST_ON_DUPE.put(GachaRarity.LEGENDARY, 200);
    }

    private GachaEconomy() {
    }

    public static Map<GachaRarity, Double> baseRates() {
        return Map.copyOf(BASE_RATES);
    }

    public static int dustFor(GachaRarity rarity) {
        return DUST_ON_DUPE.get(rarity);
    }

    public static Map<GachaRarity, Double> ratesForPity(int pityCounterAfterIncrement) {
        Map<GachaRarity, Double> rates = new EnumMap<>(GachaRarity.class);
        rates.putAll(BASE_RATES);

        if (pityCounterAfterIncrement >= HARD_PITY_AT) {
            rates.put(GachaRarity.COMMON, 0.0);
            rates.put(GachaRarity.RARE, 0.0);
            rates.put(GachaRarity.EPIC, 0.0);
            rates.put(GachaRarity.LEGENDARY, 1.0);
            return rates;
        }

        if (pityCounterAfterIncrement > SOFT_PITY_START) {
            int softPulls = pityCounterAfterIncrement - SOFT_PITY_START;
            double bonus = softPulls * SOFT_PITY_LEGENDARY_BONUS;
            double legendary = Math.min(1.0, BASE_RATES.get(GachaRarity.LEGENDARY) + bonus);
            double common = Math.max(0.0, BASE_RATES.get(GachaRarity.COMMON) - bonus);
            rates.put(GachaRarity.LEGENDARY, legendary);
            rates.put(GachaRarity.COMMON, common);
        }
        return rates;
    }

    public static Map<GachaRarity, Double> rarePlusRates() {
        Map<GachaRarity, Double> rates = new EnumMap<>(GachaRarity.class);
        double rare = BASE_RATES.get(GachaRarity.RARE);
        double epic = BASE_RATES.get(GachaRarity.EPIC);
        double legendary = BASE_RATES.get(GachaRarity.LEGENDARY);
        double total = rare + epic + legendary;
        rates.put(GachaRarity.COMMON, 0.0);
        rates.put(GachaRarity.RARE, rare / total);
        rates.put(GachaRarity.EPIC, epic / total);
        rates.put(GachaRarity.LEGENDARY, legendary / total);
        return rates;
    }

    /**
     * Salary → ★ for current FanDuel-ish slate (~11–34). Top salaries (≥29) are 5★.
     */
    public static int playerStar(Integer dollarValue) {
        int value = dollarValue == null ? 0 : dollarValue;
        if (value <= 14) {
            return 1;
        }
        if (value <= 19) {
            return 2;
        }
        if (value <= 24) {
            return 3;
        }
        if (value <= 28) {
            return 4;
        }
        return 5;
    }

    /** Equal chance for every player; salary no longer biases pulls. */
    public static int weight(Integer dollarValue) {
        return WEIGHT_FLOOR;
    }

    /** Roster salary discount from highest owned skin rarity (Common / none = 0). */
    public static int salaryDiscount(GachaRarity rarity) {
        if (rarity == null) {
            return 0;
        }
        return switch (rarity) {
            case RARE -> 1;
            case EPIC -> 2;
            case LEGENDARY -> 3;
            case COMMON -> 0;
        };
    }

    public static int salaryDiscount(String rarity) {
        if (rarity == null || rarity.isBlank()) {
            return 0;
        }
        try {
            return salaryDiscount(GachaRarity.fromString(rarity.trim()));
        } catch (IllegalArgumentException ignored) {
            return 0;
        }
    }

    /**
     * Cap-billed salary: face value minus skin discount, floored at 1 when face &gt; 0.
     */
    public static int billedSalary(Integer faceDollarValue, int discount) {
        int face = faceDollarValue == null ? 0 : faceDollarValue;
        if (face <= 0) {
            return 0;
        }
        int safeDiscount = Math.max(0, discount);
        return Math.max(1, face - safeDiscount);
    }

    public static int billedSalary(Integer faceDollarValue, String rarity) {
        return billedSalary(faceDollarValue, salaryDiscount(rarity));
    }

    public static int dailyPodiumAmount(int rank) {
        return switch (rank) {
            case 1 -> GUILD_DAILY_1ST;
            case 2 -> GUILD_DAILY_2ND;
            case 3 -> GUILD_DAILY_3RD;
            default -> 0;
        };
    }

    public static int weeklyPodiumAmount(int rank) {
        return switch (rank) {
            case 1 -> GUILD_WEEKLY_1ST;
            case 2 -> GUILD_WEEKLY_2ND;
            case 3 -> GUILD_WEEKLY_3RD;
            default -> 0;
        };
    }
}
