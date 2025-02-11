package com.bigschlong.demo.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@Jacksonized
@AllArgsConstructor
@Table(name = "is_locked")
public class IsLocked {

    private LocalDate date;
    private Boolean isLocked;

}
