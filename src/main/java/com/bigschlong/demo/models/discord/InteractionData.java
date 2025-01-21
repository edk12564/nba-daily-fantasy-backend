package com.bigschlong.demo.models.discord;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
@Builder
public class InteractionData {
    String id;
    String name;
    Options[] options;

    @Data
    @Jacksonized
    @Builder
    public static class Options {
        String name;
        Integer type;
        String value;
    }
}


