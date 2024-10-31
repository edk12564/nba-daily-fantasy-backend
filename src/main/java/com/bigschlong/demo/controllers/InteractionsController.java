package com.bigschlong.demo.controllers;

import com.bigschlong.demo.interceptors.CheckSignature;
import com.bigschlong.demo.models.PingModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/interactions")
public class InteractionsController {

    private final ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    @PostMapping(value = "/", produces = "application/json")
    public PingModel ping(HttpServletRequest request) {
        String body = CheckSignature.checkSignature(request);
        if (body == null) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(401));
        }
        var pingModel = mapper.readValue(body, PingModel.class);

        if (pingModel.type == 1) {
            return new PingModel(1);
        }
        return null;
    }

}
