package com.picknroll.demo.services;

import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.repositories.IsLockedRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IsLockedServicesTest {

    @Mock
    private IsLockedRepository isLockedRepository;

    @InjectMocks
    private IsLockedServices services;

    @Test
    @DisplayName("isTodayLocked returns true when lockTime is in the future")
    void isTodayLocked_true() {
        IsLocked row = IsLocked.builder().date(LocalDate.now()).lockTime(OffsetDateTime.now().plusMinutes(5)).build();
        given(isLockedRepository.isTodayLocked()).willReturn(Optional.of(row));
        assertTrue(services.isTodayLocked());
    }

    @Test
    @DisplayName("isTodayLocked returns false when no row or past lockTime")
    void isTodayLocked_false() {
        given(isLockedRepository.isTodayLocked()).willReturn(Optional.empty());
        assertFalse(services.isTodayLocked());

        IsLocked past = IsLocked.builder().date(LocalDate.now()).lockTime(OffsetDateTime.now().minusMinutes(1)).build();
        given(isLockedRepository.isTodayLocked()).willReturn(Optional.of(past));
        assertFalse(services.isTodayLocked());
    }

    @Test
    @DisplayName("findLock returns the row when lock time is present")
    void findLock_specificDate() {
        LocalDate d = LocalDate.parse("2025-12-25");
        IsLocked row = IsLocked.builder().date(d).lockTime(OffsetDateTime.now()).build();
        given(isLockedRepository.isLocked(d)).willReturn(Optional.of(row));
        assertEquals(d, services.findLock(d).orElseThrow().getDate());
    }

    @Test
    @DisplayName("findLock is empty when the row is missing or lock time is null")
    void findLock_missingOrMalformed() {
        LocalDate d = LocalDate.parse("2026-10-20");
        given(isLockedRepository.isLocked(d)).willReturn(Optional.empty());
        assertTrue(services.findLock(d).isEmpty());

        IsLocked malformed = IsLocked.builder().date(d).lockTime(null).build();
        given(isLockedRepository.isLocked(d)).willReturn(Optional.of(malformed));
        assertTrue(services.findLock(d).isEmpty());
    }
}
