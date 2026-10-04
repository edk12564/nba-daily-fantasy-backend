package com.picknroll.demo.services;

import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.repositories.DailyRosterRepository;
import com.picknroll.demo.repositories.DiscordChannelRepository;
import com.picknroll.demo.services.RosterWriteOutcome.Status;
import com.picknroll.demo.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DailyRosterServices {

    @Autowired
    private DailyRosterRepository dailyRosterRepository;
    @Autowired
    private DiscordChannelRepository discordChannelRepository;
    @Autowired
    private IsLockedServices isLockedServices;
    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Transactional
    public RosterWriteOutcome saveRosterChoiceWithinCap(UUID nbaPlayerUid, String discordPlayerId, String nickname,
                                                        String position, LocalDate date, int maxDollars) {
        if (date == null || discordPlayerId == null) {
            return RosterWriteOutcome.of(Status.NO_GAMES);
        }
        lockRoster(discordPlayerId, date);
        Optional<RosterWriteOutcome> closed = rejectionForLock(date);
        if (closed.isPresent()) {
            return closed.get();
        }
        int price = getTodaysRosterPriceWithPlayer(discordPlayerId, position, date, nbaPlayerUid);
        if (price > maxDollars) {
            return RosterWriteOutcome.overCap(price);
        }
        Integer changed = saveRosterChoice(nbaPlayerUid, discordPlayerId, nickname, position, date);
        if (changed != null && changed == 1) {
            return RosterWriteOutcome.of(Status.SAVED);
        }
        return RosterWriteOutcome.of(Status.NOT_CHANGED);
    }

    public void deleteRosterPlayer(DailyRosterPlayer dailyRosterPlayer) {
        dailyRosterRepository.deleteRosterPlayerByDateAndDiscordIdAndPlayerName(dailyRosterPlayer.getDate(), dailyRosterPlayer.getDiscordPlayerId(), dailyRosterPlayer.getNbaPlayerUid());
    }

    @Transactional
    public RosterWriteOutcome deleteRosterPlayerIfOpen(DailyRosterPlayer dailyRosterPlayer) {
        if (dailyRosterPlayer.getDate() == null || dailyRosterPlayer.getDiscordPlayerId() == null) {
            return RosterWriteOutcome.of(Status.NO_GAMES);
        }
        lockRoster(dailyRosterPlayer.getDiscordPlayerId(), dailyRosterPlayer.getDate());
        Optional<RosterWriteOutcome> closed = rejectionForLock(dailyRosterPlayer.getDate());
        if (closed.isPresent()) {
            return closed.get();
        }
        deleteRosterPlayer(dailyRosterPlayer);
        return RosterWriteOutcome.of(Status.DELETED);
    }

    private Optional<RosterWriteOutcome> rejectionForLock(LocalDate date) {
        Optional<IsLocked> lock = isLockedServices.findLock(date);
        if (lock.isEmpty()) {
            return Optional.of(RosterWriteOutcome.of(Status.NO_GAMES));
        }
        if (lock.get().getLockTime().isBefore(OffsetDateTime.now())) {
            return Optional.of(RosterWriteOutcome.of(Status.LOCKED));
        }
        return Optional.empty();
    }

    private void lockRoster(String discordPlayerId, LocalDate date) {
        jdbcTemplate.execute(
                "SELECT pg_advisory_xact_lock(hashtext(?), hashtext(?))",
                (PreparedStatementCallback<Void>) statement -> {
                    statement.setString(1, discordPlayerId);
                    statement.setString(2, date.toString());
                    statement.execute();
                    return null;
                });
    }

    /* Leaderboards */
    public List<DailyRosterPlayer> getGuildLeaderboard(String guildId, LocalDate date) {
        return dailyRosterRepository.getTodaysLeaderboardByGuildId(guildId, date);
    }

    public List<DailyRosterPlayer> getWeeklyGuildLeaderboard(String guildId, LocalDate date) {
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return dailyRosterRepository.getWeeksLeaderboardByGuildId(guildId, weekStart, date);
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
