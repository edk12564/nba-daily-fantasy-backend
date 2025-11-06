package com.picknroll.demo.services;

import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.repositories.DailyRosterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class DailyRosterServices {

    @Autowired
    private DailyRosterRepository dailyRosterRepository;


    // String version
    public List<String> getPlayerRostersStrings(String discordId, String guildId) {
        return dailyRosterRepository.getTodaysRosterByDiscordIdAndGuildId(discordId, guildId, LocalDate.now()).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getName() + " " + dailyRosterPlayer.getDollarValue().toString())
                .toList();
    }

    // Entity version
    public List<DailyRosterPlayer> getPlayerRoster(String discordId, String guildId, LocalDate date) {
        return dailyRosterRepository.getTodaysRosterByDiscordIdAndGuildId(discordId, guildId, date);
    }

    public List<String> getGuildRostersString(String guildId) {
        return dailyRosterRepository.getTodaysRostersByGuildId(guildId).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getNickname() + " chose " + dailyRosterPlayer.getPosition().toString() + " " + dailyRosterPlayer.getName() + " " + dailyRosterPlayer.getDollarValue().toString())
                .toList();
    }

    public List<DailyRosterPlayer> getLeaderboard(String guildId, LocalDate date) {
        return dailyRosterRepository.getTodaysRostersByGuildIdWithFantasyScore(guildId, date);
    }

    // You need to use collect() here instead of toList() because toList() creates an immutable list and therefore you are unable to .add.
    public List<String> getLeaderboardString(String guildId) {
        List<String> leaderboard = dailyRosterRepository.getTodaysRostersByGuildIdWithFantasyScore(guildId, LocalDate.now()).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getNickname() + " chose " + dailyRosterPlayer.getName() + " (" + dailyRosterPlayer.getPosition().toString() + ") - " + dailyRosterPlayer.getFantasyScore().toString())
                .collect(Collectors.toCollection(ArrayList::new));
        leaderboard.addFirst("Leaderboard:");

        return leaderboard;
    }

    public void saveRosterChoice(UUID nbaPlayerUid, String discordPlayerId, String guildId, String nickname, String position, LocalDate date) {
        dailyRosterRepository.saveRosterChoice(nbaPlayerUid, discordPlayerId, guildId, nickname, position, date);
    }


    public Integer getTodaysRosterPrice(String discordId, String guildId, String position, LocalDate date) {
        return dailyRosterRepository.getTodaysRosterPrice(discordId, guildId, position,
                date).stream().reduce(0, Integer::sum);
    }


    public Double getTodaysRosterFantasyScore(String discordId, String guildId) {
        AtomicReference<Double> result = new AtomicReference<>(0.0);
        dailyRosterRepository.getTodaysRosterFantasyScores(discordId, guildId).forEach(score -> {
            result.updateAndGet(v -> v + score);
        });
        return result.get();
    }

    public void deleteRosterPlayer(DailyRosterPlayer dailyRosterPlayer) {
        dailyRosterRepository.deleteRosterPlayerByGuildIdAndDateAndDiscordIdAndPlayerName(dailyRosterPlayer.getGuildId(), dailyRosterPlayer.getDate(), dailyRosterPlayer.getDiscordPlayerId(), dailyRosterPlayer.getNbaPlayerUid());
    }

    public List<DailyRosterPlayer> getWeeklyLeaderboard(String guildId, LocalDate date) {
        LocalDate previousMonday = date.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        return dailyRosterRepository.getWeeksLeaderboard(previousMonday, date, guildId);

    }

    public List<DailyRosterPlayer> getGlobalLeaderboard(LocalDate date) {
        return dailyRosterRepository.getTodaysGlobalRosters(date);
    }

    public Optional<DailyRosterPlayer> getRosterPlayersByDiscordIdAndPosition(String discordId, LocalDate date, String position) {
        Optional<DailyRosterPlayer> playerAtPosition = dailyRosterRepository.getRosterPlayerByDiscordIdAndPosition(discordId, date, position);
        return playerAtPosition;
    }

    public List<String> getGuildsByDiscordPlayerId(String discordPlayerId) {
        return dailyRosterRepository.getGuildsByDiscordPlayerId(discordPlayerId);
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
