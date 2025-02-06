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

    public void saveRosterChoice(NbaPlayer nbaPlayer, String discordPlayerId, String guildId, String nickname, String position) {
        dailyRosterRepository.saveRosterChoice(nbaPlayer.getNba_player_uid(), discordPlayerId, guildId, nickname, position);
    }

    public List<DailyRosterPlayer> getTodaysPlayersOnRosterByPosition(String discordId, String guildId, String position) {
        return dailyRosterRepository.getAllTodaysPlayersOnRosterByPosition(discordId, guildId, position);
    }

}
