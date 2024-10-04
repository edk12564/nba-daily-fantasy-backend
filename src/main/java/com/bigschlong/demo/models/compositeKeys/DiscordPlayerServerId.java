package com.bigschlong.demo.models.compositeKeys;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

@Data
//@Embeddable
public class DiscordPlayerServerId implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 920478969601310848L;

    private UUID discordPlayerUid;

    private UUID serverUid;

    @Override
    public int hashCode() {
        return Objects.hash(discordPlayerUid, serverUid);
    }

}