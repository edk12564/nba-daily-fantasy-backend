package com.picknroll.demo.services;

import com.picknroll.demo.models.dtos.NbaPlayer;
import com.picknroll.demo.models.joinTables.NbaPlayerTeam;
import com.picknroll.demo.repositories.NbaPlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NbaPlayerServicesTest {

    @Mock
    private NbaPlayerRepository nbaPlayerRepository;

    @InjectMocks
    private NbaPlayerServices services;

    @Test
    @DisplayName("findNbaPlayerByName delegates to repository")
    void findByName() {
        NbaPlayer p = new NbaPlayer();
        p.setName("LeBron James");
        given(nbaPlayerRepository.findNbaPlayerByName("LeBron James")).willReturn(p);
        assertEquals("LeBron James", services.findNbaPlayerByName("LeBron James").getName());
    }

    @Test
    @DisplayName("getNbaPlayersWithTeam passes string date and returns list")
    void getWithTeam() {
        LocalDate d = LocalDate.parse("2025-12-25");
        NbaPlayerTeam row = new NbaPlayerTeam();
        row.setPlayer_name("LeBron James");
        given(nbaPlayerRepository.getNbaPlayersWithTeam(eq("2025-12-25"))).willReturn(List.of(row));
        List<NbaPlayerTeam> out = services.getNbaPlayersWithTeam(d);
        assertEquals(1, out.size());
        assertEquals("LeBron James", out.get(0).getPlayer_name());
    }

    @Test
    @DisplayName("getAllTodaysNbaPlayers returns list from repository")
    void getAll() {
        NbaPlayer p = new NbaPlayer();
        p.setName("Nikola Jokic");
        given(nbaPlayerRepository.getAllTodaysNbaPlayers()).willReturn(List.of(p));
        assertEquals(1, services.getAllTodaysNbaPlayers().size());
    }

    @Test
    @DisplayName("findNbaPlayerByName returns null when not found")
    void findByName_notFound() {
        given(nbaPlayerRepository.findNbaPlayerByName("Unknown Player")).willReturn(null);
        NbaPlayer result = services.findNbaPlayerByName("Unknown Player");
        assertNull(result);
    }

    @Test
    @DisplayName("getNbaPlayersWithTeam returns empty list when no players")
    void getWithTeam_empty() {
        LocalDate d = LocalDate.parse("2025-12-25");
        given(nbaPlayerRepository.getNbaPlayersWithTeam("2025-12-25")).willReturn(List.of());
        List<NbaPlayerTeam> out = services.getNbaPlayersWithTeam(d);
        assertTrue(out.isEmpty());
    }

    @Test
    @DisplayName("getAllTodaysNbaPlayers returns empty list when no players")
    void getAll_empty() {
        given(nbaPlayerRepository.getAllTodaysNbaPlayers()).willReturn(List.of());
        List<NbaPlayer> result = services.getAllTodaysNbaPlayers();
        assertTrue(result.isEmpty());
    }
}
