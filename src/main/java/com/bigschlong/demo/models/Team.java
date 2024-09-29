package com.bigschlong.demo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "teams")
public class Team {
    @Id
    @Column(name = "team_id", nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
    private String name;

}