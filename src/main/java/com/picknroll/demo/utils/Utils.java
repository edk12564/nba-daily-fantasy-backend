package com.picknroll.demo.utils;

import com.picknroll.demo.models.discord.Interaction;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.ZoneId;

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

    @NotNull
    public static LocalDate getCaliforniaDate() {
        ZoneId californiaZone = ZoneId.of("America/Los_Angeles");
        return LocalDate.now(californiaZone);
    }
}