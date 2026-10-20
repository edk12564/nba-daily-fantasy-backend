package com.picknroll.demo.models.gacha;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class GachaWalletResponse {
    private int rolls;
    private int dust;
    private int pityCounter;
    private int streakDays;
    private LocalDate streakLastDate;
}
