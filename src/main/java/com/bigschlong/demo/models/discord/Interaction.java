package com.bigschlong.demo.models.discord;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class Interaction {
    InteractionType type;
    int version;
    String token;
    String id;
    String applicationId;

    String channelId;
    //    GUILD	0	Interaction can be used within servers
//    BOT_DM	1	Interaction can be used within DMs with the app's bot user
//    PRIVATE_CHANNEL	2 Interaction can be used within Group DMs and DMs other than the app's bot user
    int context;
    // Set if send from guild
    String guildId;
    // Set if send from guild
    String guildLocale;
    // Available on everything but ping
    String locale;
    String appPermissions;

    InteractionData data;
    // Exists if invoked in a server
    Member member;
    // Exists if invoked in DM
    User user;


    public enum InteractionType {
        PING(1),
        APPLICATION_COMMAND(2),
        MESSAGE_COMPONENT(3),
        APPLICATION_COMMAND_AUTOCOMPLETE(4),
        MODAL_SUBMIT(5);

        @JsonValue
        public final int value;

        InteractionType(int value) {
            this.value = value;
        }

    }
}
