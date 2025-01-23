package com.bigschlong.demo.models.discord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class InteractionResponse {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PingModel extends InteractionResponse {
        public Integer type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectMenu extends InteractionResponse {

        private Integer type; // Type of select menu component (3, 5, 6, 7, 8)
        private String customId; // ID for the select menu
        private List<SelectOption> options; // Specified choices (for type 3)
        private List<String> channelTypes; // List of channel types (for type 8)
        private String placeholder; // Placeholder text if nothing is selected
        private List<SelectDefaultValue> defaultValues; // Default values for auto-populated components
        private Integer minValues; // Minimum number of items that must be chosen
        private Integer maxValues; // Maximum number of items that can be chosen
        private Boolean disabled; // Whether the select menu is disabled

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SelectOption {
            private String label; // User-facing name of the option
            private String value; // Dev-defined value of the option
            private String description; // Additional description of the option
            private Emoji emoji; // Emoji object for the option
            private Boolean isDefault; // Whether this option is selected by default
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SelectDefaultValue {
            private String id; // ID of a user, role, or channel
            private String type; // Type of the value ("user", "role", or "channel")
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Emoji {
            private String id; // ID of the emoji
            private String name; // Name of the emoji
            private Boolean animated; // Whether the emoji is animated
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message extends InteractionResponse {
        private Integer type; // Type of message component (1, 2, 3, 4, 5)
        private String message; // Message to send
    }
}
