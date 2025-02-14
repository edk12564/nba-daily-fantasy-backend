package com.bigschlong.demo.controllers;

import com.bigschlong.demo.models.dtos.SetPlayerDTO;
import com.bigschlong.demo.models.joinTables.DailyRosterPlayer;
import com.bigschlong.demo.models.joinTables.NbaPlayerTeam;
import com.bigschlong.demo.services.DailyRosterServices;
import com.bigschlong.demo.services.IsLockedServices;
import com.bigschlong.demo.services.NbaPlayerServices;
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

import java.util.List;


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


    @Value("${discord.client.id}")
    private String clientId;

    @Value("${discord.client.secret}")
    private String discordClientSecret;

    OkHttpClient client = new OkHttpClient();

    @GetMapping(value = "/todays-players", produces = "application/json")
    public List<NbaPlayerTeam> getPlayers() {
        return nbaPlayerServices.getNbaPlayersWithTeam();
    }

    @GetMapping(value = "/my-roster/{guildId}/{playerId}")
    public List<DailyRosterPlayer> myRoster(@PathVariable String guildId, @PathVariable String playerId) {
        return dailyRosterServices.getPlayerRoster(playerId, guildId);
    }

    @PostMapping(value = "/set-player")
    public ResponseEntity<String> setPlayer(@RequestBody SetPlayerDTO setPlayerDTO) {
        if (isLockedServices.isTodayLocked().getIsLocked()) {
            return new ResponseEntity<>("{\"error\": \"Its past the lock time\"}", HttpStatus.BAD_REQUEST);
        }
        var currentPrice = dailyRosterServices.getTodaysRosterPrice(setPlayerDTO.getDiscord_player_id(), setPlayerDTO.getGuild_id(), setPlayerDTO.getPosition());
        if (currentPrice > MAX_DOLLARS) {
            return new ResponseEntity<>(STR."{\"error\": \"Too expensive: Current price is \{currentPrice}\"}", HttpStatus.BAD_REQUEST);
        }
        dailyRosterServices.saveRosterChoice(setPlayerDTO.getNba_player_uid(), setPlayerDTO.getDiscord_player_id(),
                setPlayerDTO.getGuild_id(), setPlayerDTO.getNickname(), setPlayerDTO.getPosition());
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
}




