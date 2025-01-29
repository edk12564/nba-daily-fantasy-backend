package com.bigschlong.demo.models.discord;

import com.bigschlong.demo.models.discord.components.Components;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class InteractionResponse {
    int type;
    InteractionResponseData data;

    @Data
    @Builder
    public static class InteractionResponseData {
        String content;
        Components[] components;
    }
}
