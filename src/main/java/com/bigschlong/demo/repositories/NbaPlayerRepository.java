package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.NbaPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NbaPlayerRepository extends JpaRepository<NbaPlayer, UUID> {



}


