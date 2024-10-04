package com.bigschlong.demo.models.compositeKeys;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

// This does getters, setters, and equals
@Data
public class DailyRosterId implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = -1329777488309363465L;
    private UUID discordPlayerUid;

    private UUID nbaPlayerUid;



}