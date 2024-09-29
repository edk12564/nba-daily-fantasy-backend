package com.bigschlong.demo.models.compositeKeys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Embeddable
public class DailyRosterId implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = -1329777488309363465L;
    @Column(name = "discord_player_uid")
    private UUID discordPlayerUid;

    @Column(name = "nba_player_uid")
    private UUID nbaPlayerUid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        DailyRosterId entity = (DailyRosterId) o;
        return Objects.equals(this.nbaPlayerUid, entity.nbaPlayerUid) &&
                Objects.equals(this.discordPlayerUid, entity.discordPlayerUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nbaPlayerUid, discordPlayerUid);
    }

}