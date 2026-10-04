package com.picknroll.demo.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picknroll.demo.httpclient.NbaAPIClient;
import com.picknroll.demo.interceptors.JwtInterceptor;
import com.picknroll.demo.models.dtos.SetPlayerDTO;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.models.joinTables.NbaPlayerTeam;
import com.picknroll.demo.services.DailyRosterServices;
import com.picknroll.demo.services.DiscordPlayerGuildServices;
import com.picknroll.demo.services.IsLockedServices;
import com.picknroll.demo.services.JwtService;
import com.picknroll.demo.services.NbaPlayerServices;
import com.picknroll.demo.services.RosterWriteOutcome;
import com.picknroll.demo.utils.Utils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("api/activity")
@CrossOrigin(origins = {"https://1290520169185280062.discordsays.com", "https://1308185877419130891.discordsays.com"})
public class ActivitiesController {

    public static final int MAX_DOLLARS = 100;
    static final String NO_GAMES_BODY = "{\"error\":\"No games scheduled\"}";
    static final String LOCKED_BODY = "{\"error\": \"Its past the lock time\"}";
    static final String FORBIDDEN_BODY = "{\"error\":\"Forbidden\"}";

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
    @Autowired
    JwtService jwtService;

    @Value("${discord.client.id}")
    private String clientId;
    @Value("${discord.client.secret}")
    private String discordClientSecret;

    OkHttpClient client = new OkHttpClient();

    @GetMapping(value = "/todays-players", produces = "application/json")
    public List<NbaPlayerTeam> getPlayers(@RequestParam Optional<LocalDate> date) {
        return nbaPlayerServices.getNbaPlayersWithTeam(date.orElse(Utils.getCaliforniaDate()));
    }

    @GetMapping(value = "/livedata/{gameId}")
    public String getLiveGameData(@PathVariable String gameId) {
        return nbaAPIClient.getGamesData(gameId);
    }

    //  This is where you look at your roster for the first time. So this is the entrypoint. Here, I should fill in the players before displaying player roster.
//  This is mainly for people starting it up in a new server. You also have to do for people changing their guild roster to also change every guild roster. this is done below is setplayer.
//  If you do the above, every player for a position should be the same across all guilds. So
    @GetMapping(value = "/my-roster/{guildId}/{channelId}/{discordPlayerId}")
    public List<DailyRosterPlayer> myRoster(@PathVariable String guildId, @PathVariable String discordPlayerId,
                                            @PathVariable String channelId,
                                            @RequestParam Optional<LocalDate> date) {
        discordPlayerGuildServices.insertGuildForPlayerId(discordPlayerId, guildId);
        discordPlayerGuildServices.insertChannelForDate(channelId, guildId);
        return dailyRosterServices.getPlayerRoster(discordPlayerId, date.orElse(date.orElse(Utils.getCaliforniaDate())));
    }

    @PostMapping(value = "/my-roster")
    public ResponseEntity<String> setPlayer(@RequestBody SetPlayerDTO setPlayerDTO, HttpServletRequest request) {
        ResponseEntity<String> forbidden = requireCurrentUser(request, setPlayerDTO.getDiscord_player_id());
        if (forbidden != null) {
            return forbidden;
        }
        RosterWriteOutcome outcome = dailyRosterServices.saveRosterChoiceWithinCap(
                setPlayerDTO.getNba_player_uid(),
                setPlayerDTO.getDiscord_player_id(),
                setPlayerDTO.getNickname(),
                setPlayerDTO.getPosition(),
                Utils.getCaliforniaDate(),
                MAX_DOLLARS);
        return mutationResponse(outcome, "OK");
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
    public ResponseEntity<?> getLockTime(@RequestParam Optional<LocalDate> date) {
        return isLockedServices.findLock(date.orElse(Utils.getCaliforniaDate()))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(NO_GAMES_BODY, HttpStatus.NOT_FOUND));
    }

    // Exchange Discord auth code for access token, fetch user data, and return a JWT
    @PostMapping(value = "/token")
    @SneakyThrows
    public ResponseEntity<String> getToken(@RequestBody String code) {
        ObjectMapper objectMapper = new ObjectMapper();

        // Get the access token from Discord
        var formBody = new FormBody.Builder()
                .add("code", code)
                .add("client_secret", discordClientSecret)
                .add("client_id", clientId)
                .add("redirect_uri", "https://picknrolls.click")
                .add("grant_type", "authorization_code").build();

        Request tokenRequest = new Request.Builder()
                .url("https://discord.com/api/oauth2/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        String accessToken;
        try (Response response = client.newCall(tokenRequest).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return new ResponseEntity<>("{\"error\": \"Failed to get access token from Discord\"}", HttpStatus.UNAUTHORIZED);
            }
            String tokenResponse = response.body().string();
            Map<String, Object> tokenData = objectMapper.readValue(tokenResponse, new TypeReference<>() {});
            accessToken = (String) tokenData.get("access_token");
            if (accessToken == null) {
                return new ResponseEntity<>("{\"error\": \"No access token in Discord response\"}", HttpStatus.UNAUTHORIZED);
            }
        }

        // Fetch user data from Discord
        Request userRequest = new Request.Builder()
                .url("https://discord.com/api/users/@me")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(userRequest).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return new ResponseEntity<>("{\"error\": \"Failed to get user data from Discord\"}", HttpStatus.UNAUTHORIZED);
            }
            String userDataJson = response.body().string();
            Map<String, Object> userData = objectMapper.readValue(userDataJson, new TypeReference<>() {});

            // Generate JWT from user data
            String jwt = jwtService.generateToken(userData);
            return new ResponseEntity<>("{\"token\": \"" + jwt + "\", \"access_token\": \"" + accessToken + "\"}", HttpStatus.OK);
        }

    }


    // Use the token to get user information
    // We might have to change this. unsure how long the discord access token lasts. Google says 7 days.
    // Better to use a JWT though.
//    @SneakyThrows
//    @PostMapping(value = "/user")
//    public String getUserInfo(@RequestBody String accessToken) {
//        var request = new Request.Builder()
//                .url("https://discord.com/api/users/@me")
//                .header("Authorization", "Bearer " + accessToken)
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            String s = response.body() != null ? response.body().string() : response.message();
//            System.out.println(s);
//            return s;
//        }
//    }

    @SneakyThrows
    @DeleteMapping(value = "/my-roster")
    public ResponseEntity<String> deleteRosterPlayer(@RequestBody DailyRosterPlayer dailyRosterPlayer,
                                                     HttpServletRequest request) {
        ResponseEntity<String> forbidden = requireCurrentUser(request, dailyRosterPlayer.getDiscordPlayerId());
        if (forbidden != null) {
            return forbidden;
        }
        RosterWriteOutcome outcome = dailyRosterServices.deleteRosterPlayerIfOpen(dailyRosterPlayer);
        return mutationResponse(outcome, "Roster player deleted successfully.");
    }

    private ResponseEntity<String> requireCurrentUser(HttpServletRequest request, String discordPlayerId) {
        Object attribute = request.getAttribute(JwtInterceptor.USER_DATA_ATTRIBUTE);
        if (!(attribute instanceof Claims claims)) {
            return new ResponseEntity<>(FORBIDDEN_BODY, HttpStatus.FORBIDDEN);
        }
        Object authenticatedId = claims.get("id");
        if (authenticatedId == null || discordPlayerId == null || !discordPlayerId.equals(authenticatedId.toString())) {
            return new ResponseEntity<>(FORBIDDEN_BODY, HttpStatus.FORBIDDEN);
        }
        return null;
    }

    private ResponseEntity<String> mutationResponse(RosterWriteOutcome outcome, String successBody) {
        return switch (outcome.status()) {
            case SAVED, DELETED -> new ResponseEntity<>(successBody, HttpStatus.OK);
            case OVER_CAP -> new ResponseEntity<>(
                    "{\"error\": \"Too expensive: total is " + outcome.totalPrice() + "\"}", HttpStatus.BAD_REQUEST);
            case LOCKED -> new ResponseEntity<>(LOCKED_BODY, HttpStatus.BAD_REQUEST);
            case NO_GAMES -> new ResponseEntity<>(NO_GAMES_BODY, HttpStatus.NOT_FOUND);
            case NOT_CHANGED -> new ResponseEntity<>("BAD request", HttpStatus.BAD_REQUEST);
        };
    }

}




