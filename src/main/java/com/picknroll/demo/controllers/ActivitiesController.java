package com.picknroll.demo.controllers;

import com.picknroll.demo.httpclient.NbaAPIClient;
import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.models.dtos.SetPlayerDTO;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.models.joinTables.NbaPlayerTeam;
import com.picknroll.demo.services.DailyRosterServices;
import com.picknroll.demo.services.IsLockedServices;
import com.picknroll.demo.services.NbaPlayerServices;
import lombok.SneakyThrows;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("api/activity")
public class ActivitiesController {

    public static final int MAX_DOLLARS = 100;

    @Autowired
    NbaPlayerServices nbaPlayerServices;
    @Autowired
    DailyRosterServices dailyRosterServices;
    @Autowired
    IsLockedServices isLockedServices;
    @Autowired
    NbaAPIClient nbaAPIClient;


    @Value("${discord.client.id}")
    private String clientId;

    @Value("${discord.client.secret}")
    private String discordClientSecret;

    OkHttpClient client = new OkHttpClient();

    @GetMapping(value = "/todays-players", produces = "application/json")
    public List<NbaPlayerTeam> getPlayers(@RequestParam Optional<LocalDate> date) {
        return nbaPlayerServices.getNbaPlayersWithTeam(date.orElse(LocalDate.now()));
    }

    @GetMapping(value = "/livedata/{gameId}")
    public String getLiveGameData(@PathVariable String gameId) {
        return nbaAPIClient.getGamesData(gameId);
    }

//  This is where you look at your roster for the first time. So this is the entrypoint. Here, I should fill in the players before displaying player roster.
//  This is mainly for people starting it up in a new server. You also have to do for people changing their guild roster to also change every guild roster. this is done below is setplayer.
//  If you do the above, every player for a position should be the same across all guilds. So
    @GetMapping(value = "/my-roster/{guildId}/{discordPlayerId}")
    public List<DailyRosterPlayer> myRoster(@PathVariable String guildId,
                                            @PathVariable String discordPlayerId,
                                            @RequestParam Optional<LocalDate> date) {

//      Need to set this at the beginning. If you keep LocalDate.now(), you might get a half filled roster on the next day if the day passes.
        LocalDate dateNow = LocalDate.now();

//        Get the players that have been chosen. if they exist, fill them in before returning the roster.
        Optional<DailyRosterPlayer> optionalPF = dailyRosterServices.checkExistingRosterPlayersByPosition(discordPlayerId, date.orElse(dateNow), "PF");
        Optional<DailyRosterPlayer> optionalC = dailyRosterServices.checkExistingRosterPlayersByPosition(discordPlayerId, date.orElse(dateNow), "C");
        Optional<DailyRosterPlayer> optionalSG = dailyRosterServices.checkExistingRosterPlayersByPosition(discordPlayerId, date.orElse(dateNow), "SG");
        Optional<DailyRosterPlayer> optionalPG = dailyRosterServices.checkExistingRosterPlayersByPosition(discordPlayerId, date.orElse(dateNow), "PG");
        Optional<DailyRosterPlayer> optionalSF = dailyRosterServices.checkExistingRosterPlayersByPosition(discordPlayerId, date.orElse(dateNow), "SF");

        List<Optional<DailyRosterPlayer>> playersList = List.of(optionalPF, optionalC, optionalSG, optionalPG, optionalSF);

        for (Optional<DailyRosterPlayer> optionalPlayer : playersList) {
            if (optionalPlayer.isPresent()) {
                DailyRosterPlayer player = optionalPlayer.get();
                dailyRosterServices.saveRosterChoice(player.getNbaPlayerUid(), discordPlayerId,
                    guildId, player.getNickname(), String.valueOf(player.getPosition()), date.orElse(dateNow));
            }
        }

        return dailyRosterServices.getPlayerRoster(discordPlayerId, guildId, date.orElse(LocalDate.now()));
    }

    // Change this to globalLeaderboard
    @GetMapping(value = "/rosters/global")
    public List<DailyRosterPlayer> globalRoster(@RequestParam LocalDate date) {
        return dailyRosterServices.getGlobalLeaderboard(date);
    }

    @GetMapping(value = "/rosters/{guildId}/weekly")
    public List<DailyRosterPlayer> guildsRostersWeekly(@PathVariable String guildId, @RequestParam LocalDate date) {
        return dailyRosterServices.getWeeklyLeaderboard(guildId, date);
    }


    @GetMapping(value = "/rosters/{guildId}")
    public List<DailyRosterPlayer> guildsRosters(@PathVariable String guildId, @RequestParam LocalDate date) {
        return dailyRosterServices.getLeaderboard(guildId, date);
    }


    @GetMapping(value = "/lock-time")
    public IsLocked getLockTime(@RequestParam Optional<LocalDate> date) {
        return isLockedServices.isLocked(date.orElse(LocalDate.now()));
    }

    @PostMapping(value = "/my-roster")
    public ResponseEntity<String> setPlayer(@RequestBody SetPlayerDTO setPlayerDTO) {
//        if (isLockedServices.isTodayLocked()) {
//            return new ResponseEntity<>("{\"error\": \"Its past the lock time\"}", HttpStatus.BAD_REQUEST);
//        }

        LocalDate date = setPlayerDTO.getDate() == null ? LocalDate.now() : setPlayerDTO.getDate();

        var currentPrice = dailyRosterServices.getTodaysRosterPrice(setPlayerDTO.getDiscord_player_id(), setPlayerDTO.getGuild_id(), setPlayerDTO.getPosition(), date);
        if (currentPrice > MAX_DOLLARS) {
            return new ResponseEntity<>("{\"error\": \"Too expensive: Current price is " + currentPrice + "\"}", HttpStatus.BAD_REQUEST);
        }

        // Set needs to set for every guild. this is because we want a global roster. if player gets changed in one guild, it needs to change everywhere.
        // Get a list of every guild the discord player is part of. Update like below but for every single guild.
        // Finally, you update the current guild if it is not in the list of guilds from your query.
        List<String> guilds = dailyRosterServices.getGuildsByDiscordPlayerId(setPlayerDTO.getDiscord_player_id());

        if (!guilds.isEmpty()) {
            for  (String guild : guilds) {
                dailyRosterServices.saveRosterChoice(setPlayerDTO.getNba_player_uid(), setPlayerDTO.getDiscord_player_id(),
                    guild, setPlayerDTO.getNickname(), setPlayerDTO.getPosition(), date);
            }
        }

        if (guilds.contains(setPlayerDTO.getGuild_id())) {
            dailyRosterServices.saveRosterChoice(setPlayerDTO.getNba_player_uid(), setPlayerDTO.getDiscord_player_id(),
                    setPlayerDTO.getGuild_id(), setPlayerDTO.getNickname(), setPlayerDTO.getPosition(), date);
        }

        return new ResponseEntity<>("Ok", HttpStatus.OK);
    }

    @PostMapping(value = "/token")
    @SneakyThrows
    public String getToken(@RequestBody String code) {
        var body = new FormBody.Builder()
                .add("code", code)
                .add("client_secret", discordClientSecret)
                .add("client_id", clientId)
                .add("grant_type", "authorization_code").build();

        Request request = new Request.Builder()
                .url("https://discord.com/api/oauth2/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : response.message();
        }
    }

    @SneakyThrows
    @PostMapping(value = "/user")
    public String getUserInfo(@RequestBody String accessToken) {
        var request = new Request.Builder()
                .url("https://discord.com/api/users/@me")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String s = response.body() != null ? response.body().string() : response.message();
            System.out.println(s);
            return s;
        }
    }

    @SneakyThrows
    @DeleteMapping(value = "/my-roster")
    public ResponseEntity<String> deleteRosterPlayer(@RequestBody DailyRosterPlayer dailyRosterPlayer) {
        dailyRosterServices.deleteRosterPlayer(dailyRosterPlayer);
        return ResponseEntity.ok("Roster player deleted successfully.");
    }

}




