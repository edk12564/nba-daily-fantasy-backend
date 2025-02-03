package com.bigschlong.demo.utils;

import com.bigschlong.demo.models.discord.Interaction;

public class GetPlayerPosition {
    public static String getPlayerPosition(Interaction interaction) {
        var position = interaction.getData().getOptions()[0].getValue();
        String pval = switch (position) {
            case "PG", "SG" -> "G";
            case "SF", "PF" -> "F";
            case "C" -> "C";
            default -> "";
        };
        return pval;
    }
}