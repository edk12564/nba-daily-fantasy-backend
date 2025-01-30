package com.bigschlong.demo.controllers;

import com.bigschlong.demo.interceptors.CheckSignature;
import com.bigschlong.demo.models.discord.Interaction;
import com.bigschlong.demo.models.discord.InteractionResponse;
import com.bigschlong.demo.models.dtos.DiscordPlayer;
import com.bigschlong.demo.services.DailyRosterServices;
import com.bigschlong.demo.services.DiscordPlayerServices;
import com.bigschlong.demo.services.NbaPlayerServices;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@RestController
@RequestMapping("/api/")
public class InteractionsController {

    private final
    NbaPlayerServices nbaPlayerServices;
    private final
    DiscordPlayerServices discordPlayerServices;
    private final
    ObjectMapper mapper = new ObjectMapper();
    private final
    DailyRosterServices dailyRosterServices;

    public InteractionsController(NbaPlayerServices nbaPlayerServices, DiscordPlayerServices discordPlayerServices, DailyRosterServices dailyRosterServices) {
        this.nbaPlayerServices = nbaPlayerServices;
        this.discordPlayerServices = discordPlayerServices;
        this.dailyRosterServices = dailyRosterServices;
    }

    @SneakyThrows
    @PostMapping(value = "/interactions", produces = "application/json")
    public InteractionResponse ping(HttpServletRequest request) {

        System.out.println("this prints");
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
            InteractionResponse build = InteractionResponse.builder().type(1).build();
            System.out.println("ping" + build);
            return build;
        }

        // now we handle the application commands using the Interaction model
        // TODO: make each of these interactions nice and pretty with the Select Menu

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
            // 1. return line to choose each position separately with set${position} interaction
            // 2. for each set${position} interaction, return a select menu that allows you to choose your player for that position
            // 3. if user has set all positions, return a message that their roster is full and do a play command to lock in their roster
            else if (Objects.equals(interaction.getData().getName(), "setroster")) {
                System.out.println("this prints set roster");
                var position = interaction.getData().getOptions()[0].getValue();
                String pval = "";

                // TODO: account for combination players like C-F, F-C, etc. Note that they are not in order either. So it could be C-F or F-C and they are equivalent.
                if (position.equals("PG") || position.equals("SG")) {
                    pval = "G";
                } else if (position.equals("SF") || position.equals("PF")) {
                    pval = "F";
                } else if (position.equals("C")) {
                    pval = "C";
                }

                var players = nbaPlayerServices.getTodaysNbaPlayersByPosition(pval).toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user resets their roster

            // interaction where user is viewing all players for all positions
            else if (Objects.equals(interaction.getData().getName(), "viewallplayers")) {
                var players = nbaPlayerServices.getAllTodaysNbaPlayers().toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content("this works!")
                        .build();
                var result = InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();

                return result;
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


        }
        var defaultdata = InteractionResponse.InteractionResponseData.builder()
                .content("This is the default response. This means that the interaction was not recognized.")
                .build();

        return InteractionResponse.builder().type(4).data(defaultdata).build();

    }

}
