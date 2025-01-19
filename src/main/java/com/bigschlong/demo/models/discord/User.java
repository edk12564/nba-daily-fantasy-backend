package com.bigschlong.demo.models.discord;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Data
public class User {
    String avatar;
    String globalName;
    String id;
    int publicFlags;
    String username;
}
