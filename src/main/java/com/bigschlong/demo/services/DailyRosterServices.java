package com.bigschlong.demo.services;

import com.bigschlong.demo.models.dtos.DailyRoster;
import com.bigschlong.demo.models.joinTables.DailyRosterPlayer;
import com.bigschlong.demo.repositories.DailyRosterRepository;
import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class DailyRosterServices {

    private final DailyRosterRepository dailyRosterRepository;

    public DailyRosterServices(DailyRosterRepository dailyRosterRepository) {
        this.dailyRosterRepository = dailyRosterRepository;
    }

    // String version
    public List<String> getPlayerRostersStrings(String discordId, String guildId) {
        return dailyRosterRepository.getTodaysRosterByDiscordIdAndGuildId(discordId, guildId).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getName() + " " + dailyRosterPlayer.getDollarValue().toString())
                .toList();
    }

    // Entity version
    public List<DailyRosterPlayer> getPlayerRoster(String discordId, String guildId) {
        return dailyRosterRepository.getTodaysRosterByDiscordIdAndGuildId(discordId, guildId);
    }

    public List<String> getGuildRostersString(String guildId) {
        return dailyRosterRepository.getTodaysRostersByGuildId(guildId).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getNickname() + " chose " + dailyRosterPlayer.getId().getPosition().toString() + " " + dailyRosterPlayer.getName() + " " + dailyRosterPlayer.getDollarValue().toString())
                .toList();
    }

    // You need to use collect() here instead of toList() because toList() creates an immutable list and therefore you are unable to .add.
    public List<DailyRosterPlayer> getLeaderboard(String guildId) {
        List<DailyRosterPlayer> leaderboard = dailyRosterRepository.getTodaysRostersByGuildIdWithFantasyScore(guildId);

        return leaderboard;
    }

    public List<String> getLeaderboardString(String guildId) {
        List <String> leaderboard = dailyRosterRepository.getTodaysRostersByGuildIdWithFantasyScore(guildId).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getNickname() + " chose " + dailyRosterPlayer.getName() + " (" + dailyRosterPlayer.getId().getPosition().toString() + ") - " + dailyRosterPlayer.getFantasyScore().toString())
                .collect(Collectors.toCollection(ArrayList::new));
        leaderboard.add(0, "Leaderboard:");

        return leaderboard;
    }

    public void saveRosterChoice(UUID nbaPlayerUid, String discordPlayerId, String guildId, String nickname, String position) {
        dailyRosterRepository.saveRosterChoice(nbaPlayerUid, discordPlayerId, guildId, nickname, position);
    }

    public List<DailyRosterPlayer> getTodaysPlayersOnRosterByPosition(String discordId, String guildId, String position) {
        return dailyRosterRepository.getTodaysRosterByPosition(discordId, guildId, position);
    }

    public Integer getTodaysRosterPrice(String discordId, String guildId) {
        AtomicInteger result = new AtomicInteger(0);
        dailyRosterRepository.getTodaysRosterPrice(discordId, guildId).forEach(price -> result.addAndGet(price));
        return result.get();
    }

    public Double getTodaysRosterFantasyScore(String discordId, String guildId) {
        AtomicReference<Double> result = new AtomicReference<>(0.0);
        dailyRosterRepository.getTodaysRosterFantasyScores(discordId, guildId).forEach(score -> result.updateAndGet(v -> v + score));
        return result.get();
    }

}

//                if (isLockedServices.isTodayLocked().getIsLocked()) {
//                    var data = InteractionResponse.InteractionResponseData.builder()
//                            .content("Today's roster is locked. You cannot make any changes.")
//                            .build();
//                    return InteractionResponse.builder()
//                            .type(4)
//                            .data(data)
//                            .build();
//                }
//
//                if (dailyRosterServices.getTodaysRosterPrice(interaction.getMember().getUser().getId(), interaction.getGuildId()) > 150) {
//                    var data = InteractionResponse.InteractionResponseData.builder()
//                            .content(String.format("You have gone over the dollar limit of $150. Make changes to your other positions or choose a cheaper %s", interaction.getData().getOptions()[0].getValue().toString()))
//                            .build();
//                    return InteractionResponse.builder()
//                            .type(4)
//                            .data(data)
//                            .build();
//                }
