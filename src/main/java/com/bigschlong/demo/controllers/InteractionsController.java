package com.bigschlong.demo.controllers;

import com.bigschlong.demo.interceptors.CheckSignature;
import com.bigschlong.demo.models.DiscordPlayer;
import com.bigschlong.demo.models.discord.InteractionResponse;
import com.bigschlong.demo.models.discord.Interaction;
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
@RequestMapping("/api/interactions")
public class InteractionsController {

    private final ObjectMapper mapper = new ObjectMapper();

    NbaPlayerServices nbaPlayerServices;
    DiscordPlayerServices discordPlayerServices;

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
            return new InteractionResponse.PingModel(1);
        }

        // now we handle the application commands using the Interaction model
        // TODO: make each of these interactions nice and pretty with the S

        // interaction where user is registering to play
        else if (interaction.getType() == Interaction.InteractionType.APPLICATION_COMMAND) {
            if (Objects.equals(interaction.getData().getName(), "register")) {
                discordPlayerServices.saveDiscordPlayer(new DiscordPlayer(interaction.getUser().getId(), interaction.getUser().getUsername()));
                return new InteractionResponse.Message(3, "You have been registered to play!");
            }

            // interaction where user is setting their roster
            // will probably consist of multiple interactions to set each position
            if (Objects.equals(interaction.getData().getName(), "setroster")) {
                var position = interaction.getData().getOptions()[0];
                return new InteractionResponse.Message(3, "hey you");
                // nbaPlayerServices.getTodaysNbaPlayersByPosition(position)
                // services to set rosters for discord players
                // return an interactionResponse with the information from nbaPlayerServices
            }
        }

        // interaction where user resets their roster

        // interaction where user is viewing all players for all positions

        // interaction where user is viewing all players

        // interaction where user is viewing their roster

        // interaction where user is viewing all discord players' nba players

        // interaction where user is checking all players last played game's scores and rankings

        return null;
    }

}
