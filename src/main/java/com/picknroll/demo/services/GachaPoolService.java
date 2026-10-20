package com.picknroll.demo.services;

import com.picknroll.demo.gacha.GachaEconomy;
import com.picknroll.demo.models.dtos.NbaPlayer;
import com.picknroll.demo.models.gacha.WeightedPlayer;
import com.picknroll.demo.repositories.NbaPlayerRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GachaPoolService {

    private final NbaPlayerRepository nbaPlayerRepository;

    public GachaPoolService(NbaPlayerRepository nbaPlayerRepository) {
        this.nbaPlayerRepository = nbaPlayerRepository;
    }

    @Cacheable(cacheNames = "gachaWeightedPool", key = "T(com.picknroll.demo.utils.Utils).getCaliforniaDate().toString()")
    public List<WeightedPlayer> loadWeightedPool() {
        List<NbaPlayer> rows = nbaPlayerRepository.findLatestDollarValuesForGachaPool();
        List<WeightedPlayer> pool = new ArrayList<>();
        for (NbaPlayer row : rows) {
            if (row.getNba_player_id() == null) {
                continue;
            }
            int dollar = row.getDollar_value() == null ? 0 : row.getDollar_value();
            pool.add(new WeightedPlayer(
                    row.getNba_player_id(),
                    dollar,
                    GachaEconomy.weight(dollar),
                    row.getName()));
        }
        return pool;
    }
}
