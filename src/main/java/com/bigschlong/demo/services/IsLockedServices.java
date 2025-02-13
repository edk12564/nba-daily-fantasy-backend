package com.bigschlong.demo.services;

import com.bigschlong.demo.repositories.IsLockedRepository;
import org.springframework.stereotype.Service;
import com.bigschlong.demo.models.dtos.IsLocked;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class IsLockedServices {

    private final IsLockedRepository isLockedRepository;
    
    public IsLockedServices(IsLockedRepository isLockedRepository) {
        this.isLockedRepository = isLockedRepository;
    }

    public IsLocked isTodayLocked() {
        IsLocked isLocked = isLockedRepository.isTodayLocked().orElse(new IsLocked(LocalDate.now(), false));
        return isLocked;
    }

}
