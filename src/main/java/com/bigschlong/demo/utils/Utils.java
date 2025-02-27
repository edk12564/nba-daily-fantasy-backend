package com.bigschlong.demo.utils;

import com.bigschlong.demo.models.discord.Interaction;

public class Utils {
    public static String getSimplePlayerPosition(Interaction interaction) {
        String position = interaction.getData().getOptions()[0].getValue();
        String result = switch (position) {
            case "PG", "SG" -> "G";
            case "SF", "PF" -> "F";
            case "C" -> "C";
            default -> "";
        };
        return result;
    }

}