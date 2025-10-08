package com.picknroll.demo.models.discord;


import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class Interaction {
    Integer type;
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

}
