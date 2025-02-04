package com.bigschlong.demo.controllers;

import com.bigschlong.demo.interceptors.CheckSignature;
import com.bigschlong.demo.models.discord.Interaction;
import com.bigschlong.demo.models.discord.InteractionResponse;
import com.bigschlong.demo.models.discord.components.Components;
import com.bigschlong.demo.services.DailyRosterServices;
import com.bigschlong.demo.services.NbaPlayerServices;
import com.bigschlong.demo.utils.GetPlayerPosition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/")
public class InteractionsController {

    @Value("${discord.client.id}")
    private String clientId;

    @Value("${discord.client.secret}")
    private String discordClientSecret;

    private final NbaPlayerServices nbaPlayerServices;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DailyRosterServices dailyRosterServices;

    OkHttpClient client = new OkHttpClient();

    public InteractionsController(NbaPlayerServices nbaPlayerServices, DailyRosterServices dailyRosterServices) {
        this.nbaPlayerServices = nbaPlayerServices;
        this.dailyRosterServices = dailyRosterServices;
    }

    @SneakyThrows
    @PostMapping(value = "/interactions", produces = "application/json")
    public InteractionResponse ping(HttpServletRequest request) {

        // configure the object mapper to ignore unknown properties instead of throwing an exception
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // check discord request signature
        String body = CheckSignature.checkSignature(request);

        // if there is no body at all, return a 401
        if (body == null) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(401));
        }

        // if there is a body, deserialize it
        Interaction interaction = mapper.readValue(body, Interaction.class);

        // if the interaction is a ping, return a ping response
        if (interaction.getType() == 1) {
            return InteractionResponse.builder().type(1).build();
        }

        // Application Commands
        // TODO: make each of these interactions nice and pretty with the Select Menu
        else if (interaction.getType() == 2) {

            // interaction where user is setting their roster
            if (Objects.equals(interaction.getData().getName(), "setroster")) {
                String playerPosition = GetPlayerPosition.getPlayerPosition(interaction);

                List<Components.SelectMenu.SelectOption> players = nbaPlayerServices.getTodaysNbaPlayersByPosition(playerPosition).stream()
                        .map(player -> Components.SelectMenu.SelectOption.builder()
                                .label(player)
                                .value(player)
                                .build())
                        .toList();
                Components selectMenu = Components.SelectMenu.builder()
                        .type(3)
                        .customId("set " + playerPosition)
                        .placeholder("Pick a player")
                        .options(players)
                        .build();
                List<Components> components = List.of(selectMenu);
                Components.ActionRow actionRow = Components.ActionRow.builder()
                        .type(1)
                        .components(components)
                        .build();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content("Choose a player for the " + playerPosition + " position")
                        .components(List.of(actionRow))
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is viewing all players for all positions
            else if (Objects.equals(interaction.getData().getName(), "viewallplayers")) {
                var players = nbaPlayerServices.getAllTodaysNbaPlayers().stream()
                        .map(player -> player.getName() + " " + player.getDollar_value()).toList().toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();

                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is viewing all players for guards
            else if (Objects.equals(interaction.getData().getName(), "viewguards")) {
                var players = nbaPlayerServices.getTodaysNbaPlayersByPosition("G").toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is viewing all players for forwards
            else if (Objects.equals(interaction.getData().getName(), "viewforwards")) {
                var players = nbaPlayerServices.getTodaysNbaPlayersByPosition("F").toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is viewing all players for centers
            else if (Objects.equals(interaction.getData().getName(), "viewcenters")) {
                var players = nbaPlayerServices.getTodaysNbaPlayersByPosition("C").toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where the user's roster is locked in and cannot be reset. This will only work when the roster is completely full.


            // interaction where user is viewing their roster
            else if (Objects.equals(interaction.getData().getName(), "roster")) {
                var players = dailyRosterServices.getPlayerRoster(interaction.getUser().getId()).toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is viewing all discord players' nba players
            else if (Objects.equals(interaction.getData().getName(), "getserverrosters")) {
                var players = dailyRosterServices.getGuildRoster(interaction.getGuildId()).toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is checking all players last played game's scores and rankings

            // Select Menu Responses
        } else if (interaction.getType() == 3) {
            var options = interaction.getData().getValues().get(0);
            String selectedValue = options;

            // Save the choice in the roster database
            dailyRosterServices.saveRosterChoice(nbaPlayerServices.findNbaPlayerByName(selectedValue.split(" ", 2)[0]), interaction.getUser().getId(), interaction.getGuildId(), interaction.getUser().getUsername());

            // Create a response confirming the selected option
            System.out.println("you made it past save");
            String content = "You have selected: " + selectedValue.split("-", 2)[0];
            var data = InteractionResponse.InteractionResponseData.builder()
                    .content(content)
                    .build();
            return InteractionResponse.builder()
                    .type(4)
                    .data(data)
                    .build();
        }
        return null;
    }

    @PostMapping(value = "/token")
    @SneakyThrows
    public String getToken(@org.springframework.web.bind.annotation.RequestBody String code) {
        RequestBody body = new FormBody.Builder()
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
    public String getUserInfo(@org.springframework.web.bind.annotation.RequestBody String accessToken) {
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