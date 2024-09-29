package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ServerRepository extends JpaRepository<Server, UUID> {

}
