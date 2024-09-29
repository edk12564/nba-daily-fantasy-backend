package com.bigschlong.demo.models;

import com.bigschlong.demo.models.joinTables.DiscordPlayerServer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "servers")
public class Server {
    @Id
    @Column(name = "server_uid", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
    private String name;
    
    // Join Table for DiscordPlayer entity using the DiscordPlayerServer entity as the join table
    @OneToMany(mappedBy = "serverId")
    private Set<DiscordPlayerServer> discordPlayerServer;

}