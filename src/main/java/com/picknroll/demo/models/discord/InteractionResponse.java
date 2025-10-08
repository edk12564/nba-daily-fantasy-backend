package com.picknroll.demo.models.discord;

import com.picknroll.demo.models.discord.components.Components;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InteractionResponse {
    int type;
    InteractionResponseData data;

    @Data
    @Builder
    public static class InteractionResponseData {
        String content;
        List<Components> components;
    }
}
