package com.bigschlong.demo.controllers;

import com.bigschlong.demo.interceptors.CheckSignature;
import com.bigschlong.demo.models.dtos.DiscordPlayer;
import com.bigschlong.demo.models.discord.InteractionResponse;
import com.bigschlong.demo.models.discord.Interaction;
import com.bigschlong.demo.services.DailyRosterServices;
import com.bigschlong.demo.services.DiscordPlayerServices;
import com.bigschlong.demo.services.NbaPlayerServices;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@RestController
@RequestMapping("/api/interactions")
public class InteractionsController {

    @Autowired
    NbaPlayerServices nbaPlayerServices;
    @Autowired
    DiscordPlayerServices discordPlayerServices;

    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private DailyRosterServices dailyRosterServices;

    @SneakyThrows
    @PostMapping(value = "/", produces = "application/json")
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
        if (interaction.getType() == Interaction.InteractionType.PING) {
            return InteractionResponse.builder().type(1).build();
        }

        // now we handle the application commands using the Interaction model
        // TODO: make each of these interactions nice and pretty with the S

        // interaction where user is registering to play
        else if (interaction.getType() == Interaction.InteractionType.APPLICATION_COMMAND) {
            if (Objects.equals(interaction.getData().getName(), "register")) {
                discordPlayerServices.saveDiscordPlayer(new DiscordPlayer(interaction.getUser().getId(), interaction.getUser().getUsername()));
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content("You have been registered to play!")
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is setting their roster
            // will probably consist of multiple interactions to set each position
            if (Objects.equals(interaction.getData().getName(), "setroster")) {

                var position = interaction.getData().getOptions()[0].getValue();
                String pval = position;
                if(position.equals("PG") || position.equals("SG")){
                    pval = "G";
                } else if (position.equals("SF") || position.equals("PF")) {
                    pval = "F";
                }
                var players = nbaPlayerServices.getTodaysNbaPlayersByPosition(pval);
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players.toString())
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
                // nbaPlayerServices.getTodaysNbaPlayersByPosition(position)
                // services to set rosters for discord players
                // return an interactionResponse with the information from nbaPlayerServices
            }
        }

            // interaction where user resets their roster

            // interaction where user is viewing all players for all positions
            if (Objects.equals(interaction.getData().getName(), "viewallplayers")) {
                var players = nbaPlayerServices.getAllTodaysNbaPlayers().toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is viewing all players for guards
            if (Objects.equals(interaction.getData().getName(), "viewguards")) {
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
            if (Objects.equals(interaction.getData().getName(), "viewforwards")) {
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
            if (Objects.equals(interaction.getData().getName(), "viewcenters")) {
                var players = nbaPlayerServices.getTodaysNbaPlayersByPosition("C").toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is viewing their roster
            if (Objects.equals(interaction.getData().getName(), "getmyroster")) {
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
            if (Objects.equals(interaction.getData().getName(), "getserverrosters")) {
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

        return null;
    }

}
