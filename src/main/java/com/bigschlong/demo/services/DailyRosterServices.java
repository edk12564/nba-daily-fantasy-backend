package com.bigschlong.demo.services;

import com.bigschlong.demo.models.dtos.DailyRoster;
import com.bigschlong.demo.models.dtos.NbaPlayer;
import com.bigschlong.demo.models.joinTables.DailyRosterPlayer;
import com.bigschlong.demo.repositories.DailyRosterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DailyRosterServices {

    private final DailyRosterRepository dailyRosterRepository;

    public DailyRosterServices(DailyRosterRepository dailyRosterRepository) {
        this.dailyRosterRepository = dailyRosterRepository;
    }

    // String version
    public List<String> getPlayerRosterString(String discordId, String guildId) {
        return dailyRosterRepository.getRosterByDiscordIdAndGuildId(discordId, guildId).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getName() + " " + dailyRosterPlayer.getDollarValue().toString())
                .toList();
    }

    // Entity version
    public List<DailyRosterPlayer> getPlayerRoster(String discordId, String guildId) {
        return dailyRosterRepository.getRosterByDiscordIdAndGuildId(discordId, guildId);
    }


    public List<String> getGuildRostersString(String guildId) {
        return dailyRosterRepository.getRostersByGuildId(guildId).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getNickname() + " chose " + dailyRosterPlayer.getPosition() + " " + dailyRosterPlayer.getName() + " " + dailyRosterPlayer.getDollarValue().toString())
                .toList();
    }

    public void saveRosterChoice(UUID nbaPlayerUid, String discordPlayerId, String guildId, String nickname, String position) {

        dailyRosterRepository.saveRosterChoice(nbaPlayerUid, discordPlayerId, guildId, nickname, position);

    }


}
