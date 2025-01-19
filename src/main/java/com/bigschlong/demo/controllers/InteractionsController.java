package com.bigschlong.demo.controllers;

import com.bigschlong.demo.interceptors.CheckSignature;
import com.bigschlong.demo.models.PingModel;
import com.bigschlong.demo.models.discord.Interaction;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class InteractionsController {

    private final ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    @PostMapping(value = "/interactions", produces = "application/json")
    public PingModel ping(HttpServletRequest request) {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String body = CheckSignature.checkSignature(request);
        if (body == null) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(401));
        }
        var pingModel = mapper.readValue(body, Interaction.class);

        if (pingModel.getType() == Interaction.InteractionType.PING) {
            return new PingModel(1);
        }
        return null;
    }

}
