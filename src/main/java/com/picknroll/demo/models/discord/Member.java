package com.picknroll.demo.models.discord;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Date;

@Data
@Builder
@Jacksonized
public class Member {
    String permissions;
    String[] roles;
    Date joinedAt;
    User user;
}
