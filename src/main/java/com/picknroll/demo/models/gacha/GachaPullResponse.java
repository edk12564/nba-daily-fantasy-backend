package com.picknroll.demo.models.gacha;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GachaPullResponse {
    private List<GachaPullResult> results;
    private int rolls;
    private int dust;
    private int pityCounter;
}
