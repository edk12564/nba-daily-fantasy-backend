package com.bigschlong.demo.controllers;

import com.bigschlong.demo.interceptors.CheckSignature;
import com.bigschlong.demo.models.discord.Interaction;
import com.bigschlong.demo.models.discord.InteractionResponse;
import com.bigschlong.demo.models.discord.components.Components;
import com.bigschlong.demo.models.dtos.IsLocked;
import com.bigschlong.demo.services.DailyRosterServices;
import com.bigschlong.demo.services.IsLockedServices;
import com.bigschlong.demo.services.NbaPlayerServices;
import com.bigschlong.demo.utils.GetSimplePlayerPosition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
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

    private final NbaPlayerServices nbaPlayerServices;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DailyRosterServices dailyRosterServices;
    private final IsLockedServices isLockedServices;


    public InteractionsController(NbaPlayerServices nbaPlayerServices, DailyRosterServices dailyRosterServices, IsLockedServices isLockedServices) {
        this.nbaPlayerServices = nbaPlayerServices;
        this.dailyRosterServices = dailyRosterServices;
        this.isLockedServices = isLockedServices;
    }

    @SneakyThrows
    @PostMapping(value = "/interactions", produces = "application/json")
    public InteractionResponse ping(HttpServletRequest request) {

        // configure the object mapper to ignore unknown properties instead of throwing an exception
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());

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
        else if (interaction.getType() == 2) {

            // interaction for info on the bot's application commands
            if (Objects.equals(interaction.getData().getName(), "info")) {
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content("Here are the available commands: \n" +
                                "/setroster - Set your roster for the day\n" +
                                "/viewallplayers - View all players for the day\n" +
                                "/viewguards - View all guards for the day\n" +
                                "/viewforwards - View all forwards for the day\n" +
                                "/viewcenters - View all centers for the day\n" +
                                "/myroster - View your roster\n" +
                                "/serverrosters - View all rosters for the server\n" +
                                "/leaderboard - View the leaderboard")
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is setting their roster
            else if (Objects.equals(interaction.getData().getName(), "setroster")) {

                if (isLockedServices.isTodayLocked().getIsLocked()) {
                    var data = InteractionResponse.InteractionResponseData.builder()
                            .content("Today's roster is locked. You cannot make any changes.")
                            .build();
                    return InteractionResponse.builder()
                            .type(4)
                            .data(data)
                            .build();
                }

                if (dailyRosterServices.getTodaysRosterPrice(interaction.getMember().getUser().getId(), interaction.getGuildId()) > 150) {
                    var data = InteractionResponse.InteractionResponseData.builder()
                            .content(String.format("You have gone over the dollar limit of $150. Make changes to your other positions or choose a cheaper %s", interaction.getData().getOptions()[0].getValue()))
                            .build();
                    return InteractionResponse.builder()
                            .type(4)
                            .data(data)
                            .build();
                }

                String playerPosition = GetSimplePlayerPosition.getSimplePlayerPosition(interaction);

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
                        .content("Choose a player for the " + interaction.getData().getOptions()[0].getValue() + " position")
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
                        .map(player -> player.getName() + " " + player.getDollar_value())
                        .toList()
                        .toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();

                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is viewing all players for guards
            else if (Objects.equals(interaction.getData().getName(), "viewpointguards")) {
                var players = nbaPlayerServices.getTodaysNbaPlayersByPosition("G").toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is viewing all players for shooting guards
            else if (Objects.equals(interaction.getData().getName(), "viewshootingguards")) {
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
            else if (Objects.equals(interaction.getData().getName(), "viewsmallforwards")) {
                var players = nbaPlayerServices.getTodaysNbaPlayersByPosition("F").toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is viewing all players for power forwards
            else if (Objects.equals(interaction.getData().getName(), "viewpowerforwards")) {
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

            // interaction where user is viewing their roster
            else if (Objects.equals(interaction.getData().getName(), "myroster")) {
                var players = dailyRosterServices.getPlayerRostersStrings(interaction.getMember().getUser().getId(), interaction.getGuildId()).toString();
                if (players.equals("[]")) {
                    players = "You have nothing in your roster. Use /setroster to set a player.";
                }
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is viewing rosters for the whole server (eventually make this prettier somehow)
            else if (Objects.equals(interaction.getData().getName(), "serverrosters")) {
                String players = dailyRosterServices.getGuildRostersString(interaction.getGuildId()).toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }

            // interaction where user is checking all players last played game's scores and rankings (leaderboard)
            else if (Objects.equals(interaction.getData().getName(), "leaderboard")) {
                String players = dailyRosterServices.getLeaderboard(interaction.getGuildId()).toString();
                var data = InteractionResponse.InteractionResponseData.builder()
                        .content(players)
                        .build();
                return InteractionResponse.builder()
                        .type(4)
                        .data(data)
                        .build();
            }
        }

        // Select Menu Responses

        // select menu response for setroster
        else if (interaction.getType() == 3) {

            String selectedValue = interaction.getData().getValues().get(0);

            // Save the choice in the roster database
            dailyRosterServices.saveRosterChoice(nbaPlayerServices.findNbaPlayerByName(selectedValue.split("-", 2)[0]).getNba_player_uid(),
                    interaction.getMember().getUser().getId(), interaction.getGuildId(), interaction.getMember().getUser().getUsername(), "PG");

            // Create a response confirming the selected option
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


}