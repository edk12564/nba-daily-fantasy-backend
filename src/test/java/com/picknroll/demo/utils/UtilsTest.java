package com.picknroll.demo.utils;

import com.picknroll.demo.models.discord.Interaction;
import com.picknroll.demo.models.discord.InteractionData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @ParameterizedTest
    @CsvSource({
            "PG, G",
            "SG, G",
            "SF, F",
            "PF, F",
            "C, C",
            "UNKNOWN, ''"
    })
    @DisplayName("getSimplePlayerPosition returns correct simplified position")
    void getSimplePlayerPosition(String input, String expected) {
        InteractionData.Option option = InteractionData.Option.builder()
                .value(input)
                .build();

        InteractionData data = InteractionData.builder()
                .options(new InteractionData.Option[]{option})
                .build();

        Interaction interaction = Interaction.builder()
                .data(data)
                .build();

        String result = Utils.getSimplePlayerPosition(interaction);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("getCaliforniaDate returns current date in California timezone")
    void getCaliforniaDate() {
        LocalDate californiaDate = Utils.getCaliforniaDate();
        assertNotNull(californiaDate);

        // Verify it's a reasonable date (matches California timezone)
        LocalDate today = LocalDate.now(ZoneId.of("America/Los_Angeles"));
        assertEquals(today, californiaDate);
    }
}
