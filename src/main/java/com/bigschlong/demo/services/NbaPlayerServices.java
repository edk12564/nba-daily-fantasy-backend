package com.bigschlong.demo.services;

import com.bigschlong.demo.repositories.NbaPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NbaPlayerServices {
    
    @Autowired
    private NbaPlayerRepository nbaPlayerRepository;
    
}
