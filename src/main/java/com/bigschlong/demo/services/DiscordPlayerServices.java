package com.bigschlong.demo.services;

import com.bigschlong.demo.repositories.DiscordPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiscordPlayerServices {

    @Autowired
    private DiscordPlayerRepository discordPlayerRepository;

}

