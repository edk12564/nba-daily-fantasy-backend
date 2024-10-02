package com.bigschlong.demo.models.compositeKeys;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
//@Embeddable
public class DiscordPlayerServerId implements java.io.Serializable {
    private static final long serialVersionUID = 920478969601310848L;
    @Column(name = "discord_player_uid")
    private UUID discordPlayerUid;

    @Column(name = "server_uid")
    private UUID serverUid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        DiscordPlayerServerId entity = (DiscordPlayerServerId) o;
        return Objects.equals(this.discordPlayerUid, entity.discordPlayerUid) &&
                Objects.equals(this.serverUid, entity.serverUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(discordPlayerUid, serverUid);
    }

}