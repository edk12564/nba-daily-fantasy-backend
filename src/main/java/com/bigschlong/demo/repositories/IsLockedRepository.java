package com.bigschlong.demo.repositories;

import com.bigschlong.demo.models.dtos.IsLocked;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IsLockedRepository extends CrudRepository<IsLocked, UUID> {

    @Query(value = """
    SELECT il.date, il.lock_time FROM is_locked il
    WHERE il.date = CURRENT_DATE
    """)
    Optional<IsLocked> isTodayLocked();

    @Query(value = """
    SELECT il.date, il.lock_time FROM is_locked il
    WHERE il.date = :date
    """)
    IsLocked isLocked(LocalDate date);
}
