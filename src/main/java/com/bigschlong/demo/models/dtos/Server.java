package com.bigschlong.demo.models.dtos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
//@Entity
@Table(name = "servers")
public class Server {
    @Id
    private UUID server_uid;
    
    private String name;

//    @Id
//    @Column(name = "server_uid", nullable = false)
//    private UUID id;
//
//    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
//    private String name;
    
    // Join Table for DiscordPlayer entity using the DiscordPlayerServer entity as the join table
//    @OneToMany(mappedBy = "serverId")
//    private Set<DiscordPlayerServer> discordPlayerServer;

}