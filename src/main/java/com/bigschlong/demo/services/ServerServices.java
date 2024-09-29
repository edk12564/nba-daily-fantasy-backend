package com.bigschlong.demo.services;

import com.bigschlong.demo.repositories.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServerServices {
    
    @Autowired
    private ServerRepository serverRepository;
    
}