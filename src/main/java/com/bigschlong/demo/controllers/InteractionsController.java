package com.bigschlong.demo.controllers;

import com.bigschlong.demo.interceptors.CheckSignature;
import com.bigschlong.demo.models.discord.Interaction;
import com.bigschlong.demo.models.discord.InteractionResponse;
import com.bigschlong.demo.models.discord.components.Components;
import com.bigschlong.demo.services.DailyRosterServices;
import com.bigschlong.demo.services.IsLockedServices;
import com.bigschlong.demo.services.NbaPlayerServices;
import com.bigschlong.demo.utils.GetSimplePlayerPosition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/interactions")
public class InteractionsController {

    private final NbaPlayerServices nbaPlayerServices;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DailyRosterServices dailyRosterServices;
    private final IsLockedServices isLockedServices;
    private final RestTemplate restTemplate;


    public InteractionsController(NbaPlayerServices nbaPlayerServices, DailyRosterServices dailyRosterServices, IsLockedServices isLockedServices, RestTemplate restTemplate) {
        this.nbaPlayerServices = nbaPlayerServices;
        this.dailyRosterServices = dailyRosterServices;
        this.isLockedServices = isLockedServices;
        this.restTemplate = restTemplate;
    }

    @SneakyThrows
    @PostMapping(value = "", produces = "application/json")
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
            // TODO: Get this shit working
            else if (Objects.equals(interaction.getData().getName(), "setroster")) {

                // Process the interaction asynchronously
                CompletableFuture.runAsync(() -> {
                    try {
                        // Extract the simplified player position from the interaction
                        String simplifiedPlayerPosition = GetSimplePlayerPosition.getSimplePlayerPosition(interaction);

                        // Retrieve the list of players and build select options
                        List<Components.SelectMenu.SelectOption> players = nbaPlayerServices
                                .getTodaysNbaPlayersByPosition(simplifiedPlayerPosition)
                                .stream()
                                .map(player -> Components.SelectMenu.SelectOption.builder()
                                        .label(player)
                                        .value(player)
                                        .build())
                                .collect(Collectors.toList());

                        // Build the select menu component
                        Components selectMenu = Components.SelectMenu.builder()
                                .type(3)
                                .customId("set " + simplifiedPlayerPosition)
                                .placeholder("Pick a player")
                                .options(players)
                                .build();

                        // Wrap the select menu in an ActionRow
                        Components.ActionRow actionRow = Components.ActionRow.builder()
                                .type(1)
                                .components(List.of(selectMenu))
                                .build();

                        // Build the interaction response data including content and components
                        InteractionResponse.InteractionResponseData data = InteractionResponse.InteractionResponseData.builder()
                                .content("Choose a player for the " + simplifiedPlayerPosition + " position")
                                .components(List.of(actionRow))
                                .build();

                        // Construct the edit URL using your application ID and the interaction token
                        String applicationId = interaction.getApplicationId(); // You might get this from config or the interaction itself
                        String editUrl = String.format(
                                "https://discord.com/api/v8/webhooks/%s/%s/messages/@original",
                                applicationId,
                                interaction.getToken()
                        );

                        // Prepare HTTP headers
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);

                        // Build the updated message payload (this can still be your type 4-like content)
                        InteractionResponse updatedResponse = InteractionResponse.builder()
                                .data(data)  // Your InteractionResponseData built earlier
                                .build();

                        HttpEntity<InteractionResponse> requestEntity = new HttpEntity<>(updatedResponse, headers);

                        // Use PATCH instead of POST
                        ResponseEntity<String> response = restTemplate.exchange(
                                editUrl,
                                HttpMethod.PATCH,
                                requestEntity,
                                String.class
                        );

                        // Check if the response status indicates success
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new ResponseStatusException(response.getStatusCode(), "Failed to send interaction followup");
                        }
                    } catch (Exception e) {
                        // Log any other exceptions that occur during processing
                        e.printStackTrace();
                    }

                    System.out.println("Asynchronous process completed.");
                });

                // Immediately return a deferred response (type 5) to comply with Discord's 3-second requirement
                return InteractionResponse.builder()
                        .type(5)
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