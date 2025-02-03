package com.bigschlong.demo.models.discord;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Jacksonized
@Builder
public class InteractionData {
    String id;
    String name;
    Option[] options;

    List<String> values;

    @Data
    @Jacksonized
    @Builder
    public static class Option {
        String name;
        Integer type;
        String value;
    }
}


