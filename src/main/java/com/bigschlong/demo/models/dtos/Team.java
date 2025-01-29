package com.bigschlong.demo.models.dtos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name = "teams")
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