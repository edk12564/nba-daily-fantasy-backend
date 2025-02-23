package com.bigschlong.demo.services;

import com.bigschlong.demo.models.dtos.IsLocked;
import com.bigschlong.demo.repositories.IsLockedRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class IsLockedServices {

    private final IsLockedRepository isLockedRepository;

    public IsLockedServices(IsLockedRepository isLockedRepository) {
        this.isLockedRepository = isLockedRepository;
    }

    public boolean isTodayLocked() {
        Optional<IsLocked> isLocked = isLockedRepository.isTodayLocked();
        return isLocked.filter(locked -> locked.getLockTime().isAfter(OffsetDateTime.now())).isPresent();
    }

    public IsLocked isLocked(LocalDate date) {
        return isLockedRepository.isLocked(date);
    }

}
