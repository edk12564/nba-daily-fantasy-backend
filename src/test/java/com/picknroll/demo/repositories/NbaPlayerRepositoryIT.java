package com.picknroll.demo.repositories;

import com.picknroll.demo.models.dtos.NbaPlayer;
import com.picknroll.demo.models.dtos.Team;
import com.picknroll.demo.models.joinTables.NbaPlayerTeam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NbaPlayerRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private NbaPlayerRepository nbaPlayerRepository;

    @Test
    void findByNameAndUid_shouldReturnExpectedPlayer() {
        NbaPlayer byName = nbaPlayerRepository.findNbaPlayerByName("LeBron James");
        assertNotNull(byName);
        assertEquals("LeBron James", byName.getName());

        UUID uid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        NbaPlayer byUid = nbaPlayerRepository.findNbaPlayerByUid(uid);
        assertNotNull(byUid);
        assertEquals(uid, byUid.getNba_player_uid());
    }

    @Test
    void getTeamByPlayerUid_shouldJoinTeams() {
        UUID lebronUid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        List<Team> teams = nbaPlayerRepository.getTeamByPlayerUid(lebronUid);
        assertNotNull(teams);
        assertEquals(1, teams.size());
        assertEquals(1, teams.get(0).getTeam_id());
    }

    @Test
    void getTodaysPlayersFiltersAndOrders() {
        List<NbaPlayer> pgs = nbaPlayerRepository.getTodaysNbaPlayersByPosition("PG");
        assertFalse(pgs.isEmpty());
        assertEquals("Stephen Curry", pgs.get(0).getName());

        List<NbaPlayer> all = nbaPlayerRepository.getAllTodaysNbaPlayers();
        assertTrue(all.size() >= 5);
        // Ordered by dollar_value DESC
        assertEquals("Nikola Jokic", all.get(0).getName());
        assertTrue(all.get(0).getDollar_value() >= all.get(1).getDollar_value());
    }

    @Test
    void getNbaPlayersWithTeam_shouldReturnJoinedProjection() {
        List<NbaPlayerTeam> rows = nbaPlayerRepository.getNbaPlayersWithTeam("2025-12-25");
        assertFalse(rows.isEmpty());
        NbaPlayerTeam first = rows.get(0);
        assertNotNull(first.getPlayer_name());
        assertNotNull(first.getTeam_name());
        assertNotNull(first.getAgainst_team_name());
        assertEquals("2025-12-25", first.getDate());
    }
}
