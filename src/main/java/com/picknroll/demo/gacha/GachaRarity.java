package com.picknroll.demo.gacha;

public enum GachaRarity {
    COMMON(1),
    RARE(2),
    EPIC(3),
    LEGENDARY(4);

    private final int rank;

    GachaRarity(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public boolean isRareOrHigher() {
        return rank >= RARE.rank;
    }

    public static GachaRarity fromString(String value) {
        return GachaRarity.valueOf(value);
    }
}
