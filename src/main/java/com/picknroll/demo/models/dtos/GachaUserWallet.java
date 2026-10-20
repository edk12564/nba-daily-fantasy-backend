package com.picknroll.demo.models.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@Jacksonized
@Table("gacha_user_wallet")
public class GachaUserWallet {

    @Id
    private String discordPlayerId;
    private int rolls;
    private int dust;
    private int pityCounter;
    private int streakDays;
    private LocalDate streakLastDate;
    private OffsetDateTime updatedAt;
}
