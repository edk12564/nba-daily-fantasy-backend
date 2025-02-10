package com.bigschlong.demo.utils;

import com.bigschlong.demo.models.discord.Interaction;

public class GetSimplePlayerPosition {
    public static String getSimplePlayerPosition(Interaction interaction) {
        var position = interaction.getData().getOptions()[0].getValue().toString();
        String pval = switch (position) {
            case "PG", "SG" -> "G";
            case "SF", "PF" -> "F";
            case "C" -> "C";
            default -> "";
        };
        return pval;
    }
}