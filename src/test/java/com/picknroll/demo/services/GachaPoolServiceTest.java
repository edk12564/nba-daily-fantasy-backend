package com.picknroll.demo.services;

import com.picknroll.demo.models.dtos.NbaPlayer;
import com.picknroll.demo.models.gacha.WeightedPlayer;
import com.picknroll.demo.repositories.NbaPlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GachaPoolServiceTest {

    @Mock
    NbaPlayerRepository nbaPlayerRepository;

    @InjectMocks
    GachaPoolService poolService;

    @Test
    void loadWeightedPoolAppliesFloorAndSkipsNullIds() {
        NbaPlayer high = new NbaPlayer();
        high.setNba_player_id(10);
        high.setDollar_value(36);

        NbaPlayer low = new NbaPlayer();
        low.setNba_player_id(11);
        low.setDollar_value(3);

        NbaPlayer missingId = new NbaPlayer();
        missingId.setDollar_value(20);

        NbaPlayer nullSalary = new NbaPlayer();
        nullSalary.setNba_player_id(12);
        nullSalary.setDollar_value(null);

        given(nbaPlayerRepository.findLatestDollarValuesForGachaPool())
                .willReturn(List.of(high, low, missingId, nullSalary));

        List<WeightedPlayer> pool = poolService.loadWeightedPool();
        assertEquals(3, pool.size());
        assertEquals(36, pool.get(0).weight());
        assertEquals(8, pool.get(1).weight());
        assertEquals(8, pool.get(2).weight());
    }
}
