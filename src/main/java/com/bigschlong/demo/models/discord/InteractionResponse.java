package com.bigschlong.demo.models.discord;

import com.bigschlong.demo.models.discord.components.Components;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.bind.annotation.BindParam;

@Data
@Builder
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
