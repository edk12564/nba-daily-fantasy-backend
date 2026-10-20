package com.picknroll.demo.models.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Jacksonized
@Table("gacha_user_skins")
public class GachaUserSkin {

    @Id
    private GachaUserSkinId id;
    private OffsetDateTime acquiredAt;

    @Getter
    @Setter
    @Builder
    @Jacksonized
    public static class GachaUserSkinId {
        private String discordPlayerId;
        private UUID skinId;
    }
}
