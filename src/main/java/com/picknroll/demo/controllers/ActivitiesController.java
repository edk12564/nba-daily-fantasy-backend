package com.picknroll.demo.controllers;

import com.picknroll.demo.httpclient.NbaAPIClient;
import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.models.joinTables.NbaPlayerTeam;
import com.picknroll.demo.services.DailyRosterServices;
import com.picknroll.demo.services.DiscordPlayerGuildServices;
import com.picknroll.demo.services.IsLockedServices;
import com.picknroll.demo.services.NbaPlayerServices;
import lombok.SneakyThrows;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Autowired
    DiscordPlayerGuildServices discordPlayerGuildServices;


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
    public List<DailyRosterPlayer> myRoster(@PathVariable String guildId, @PathVariable String discordPlayerId,
                                            @RequestParam Optional<LocalDate> date) {
        discordPlayerGuildServices.insertGuildForPlayerId(discordPlayerId, guildId);
        return dailyRosterServices.getPlayerRoster(discordPlayerId, date.orElse(date.orElse(LocalDate.now())));
    }

    // Change this to globalLeaderboard
    @GetMapping(value = "/rosters/global")
    public List<DailyRosterPlayer> globalLeaderboard(@RequestParam LocalDate date) {
        return dailyRosterServices.getGlobalLeaderboard(date);
    }

    @GetMapping(value = "/rosters/weekly/{guildId}")
    public List<DailyRosterPlayer> guildsWeeklyLeaderboard(@PathVariable String guildId, @RequestParam LocalDate date) {
        return dailyRosterServices.getWeeklyGuildLeaderboard(guildId, date);
    }

    @GetMapping(value = "/rosters/{guildId}")
    public List<DailyRosterPlayer> guildsDailyLeaderboard(@PathVariable String guildId, @RequestParam LocalDate date) {
        return dailyRosterServices.getGuildLeaderboard(guildId, date);
    }

    /* Lock Stuff */
    @GetMapping(value = "/lock-time")
    public IsLocked getLockTime(@RequestParam Optional<LocalDate> date) {
        return isLockedServices.isLocked(date.orElse(LocalDate.now()));
    }

//    Work on this at some point
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

//    Work on this at some point
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




