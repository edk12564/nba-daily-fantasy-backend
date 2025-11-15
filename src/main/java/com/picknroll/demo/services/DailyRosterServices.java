package com.picknroll.demo.services;

import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.repositories.DailyRosterRepository;
import com.picknroll.demo.repositories.DiscordChannelRepository;
import com.picknroll.demo.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
public class DailyRosterServices {

    @Autowired
    private DailyRosterRepository dailyRosterRepository;
    @Autowired
    private DiscordChannelRepository discordChannelRepository;

    /* Get Entire Rosters and Aggregates */
    // String version
    public List<String> getPlayerRostersStrings(String discordId) {
        return dailyRosterRepository.getTodaysRosterByDiscordId(discordId, Utils.getCaliforniaDate()).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getName() + " " + dailyRosterPlayer.getDollarValue().toString())
                .toList();
    }

    // Entity version
    public List<DailyRosterPlayer> getPlayerRoster(String discordId, LocalDate date) {
        return dailyRosterRepository.getTodaysRosterByDiscordId(discordId, date);
    }

    // Roster Price Sum
    public Integer getTodaysRosterPriceWithPlayer(String discordId, String position, LocalDate date, UUID nbaPlayerUid) {
        return dailyRosterRepository.getTodaysRosterPriceWithPlayer(discordId, position,
                date.toString(), nbaPlayerUid).stream().reduce(0, Integer::sum);
    }


    /* CRUD DailyRosterPlayer */
    public Integer saveRosterChoice(UUID nbaPlayerUid, String discordPlayerId, String nickname, String position, LocalDate date) {
        return dailyRosterRepository.saveRosterChoice(nbaPlayerUid, discordPlayerId, nickname, position, date.toString());
    }

    public void deleteRosterPlayer(DailyRosterPlayer dailyRosterPlayer) {
        dailyRosterRepository.deleteRosterPlayerByDateAndDiscordIdAndPlayerName(dailyRosterPlayer.getDate(), dailyRosterPlayer.getDiscordPlayerId(), dailyRosterPlayer.getNbaPlayerUid());
    }

    /* Leaderboards */
    public List<DailyRosterPlayer> getGuildLeaderboard(String guildId, LocalDate date) {
        return dailyRosterRepository.getTodaysLeaderboardByGuildId(guildId, date);
    }

    public List<DailyRosterPlayer> getWeeklyGuildLeaderboard(String guildId, LocalDate date) {
        LocalDate previousMonday = date.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        return dailyRosterRepository.getWeeksLeaderboardByGuildId(guildId, previousMonday, date);
    }

    public List<DailyRosterPlayer> getGlobalLeaderboard(LocalDate date) {
        return dailyRosterRepository.getTodaysGlobalLeaderboard(date);
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
