package com.bigschlong.demo.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
//@Entity
//@Table(name = "teams")
public class Team {
    @Id
    private Integer team_id;
    
    private String name;
    
//    @Id
//    @Column(name = "team_id", nullable = false)
//    private Integer id;
//
//    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
//    private String name;

}