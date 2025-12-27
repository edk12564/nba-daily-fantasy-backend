package com.picknroll.demo.repositories;

import com.picknroll.demo.models.dtos.IsLocked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IsLockedRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private IsLockedRepository isLockedRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Ensure today's date has a lock record for testing
        LocalDate today = LocalDate.now();
        jdbcTemplate.update(
            "INSERT INTO is_locked (date, lock_time) VALUES (?, ?) ON CONFLICT (date) DO UPDATE SET lock_time = EXCLUDED.lock_time",
            today, OffsetDateTime.now().plusHours(5)
        );
    }

    @Test
    void isTodayLocked_shouldReturnOptional() {
        Optional<IsLocked> today = isLockedRepository.isTodayLocked();
        assertTrue(today.isPresent(), "Seed data should contain CURRENT_DATE lock row");
        assertNotNull(today.get().getLockTime());
    }

    @Test
    void isLocked_specificDate() {
        LocalDate date = LocalDate.parse("2025-12-25");
        IsLocked locked = isLockedRepository.isLocked(date);
        assertNotNull(locked);
        assertEquals(date, locked.getDate());
    }
}
