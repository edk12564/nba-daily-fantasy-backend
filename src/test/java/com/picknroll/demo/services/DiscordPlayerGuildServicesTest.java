package com.picknroll.demo.services;

import com.picknroll.demo.repositories.DiscordChannelRepository;
import com.picknroll.demo.repositories.DiscordPlayerGuildRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiscordPlayerGuildServicesTest {

    @Mock
    private DiscordPlayerGuildRepository discordPlayerGuildRepository;

    @Mock
    private DiscordChannelRepository discordChannelRepository;

    @InjectMocks
    private DiscordPlayerGuildServices services;

    @Test
    @DisplayName("insertGuildForPlayerId delegates to repository")
    void insertGuildForPlayerId() {
        services.insertGuildForPlayerId("player-1", "guild-1");
        verify(discordPlayerGuildRepository).insertGuildForPlayerId("player-1", "guild-1");
    }

    @Test
    @DisplayName("insertChannelForDate delegates to repository with California date")
    void insertChannelForDate() {
        services.insertChannelForDate("chan-1", "guild-1");
        // Verify it was called with any LocalDate (since it uses Utils.getCaliforniaDate())
        verify(discordChannelRepository).insertChannelForDate(eq("chan-1"), any(LocalDate.class), eq("guild-1"));
    }
}
