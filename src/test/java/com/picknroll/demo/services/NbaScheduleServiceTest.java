package com.picknroll.demo.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picknroll.demo.httpclient.NbaScheduleClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NbaScheduleServiceTest {

    @Mock
    private NbaScheduleClient nbaScheduleClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getGamesByDate_parsesGameIdsByEstDate() {
        given(nbaScheduleClient.getLeagueSchedule()).willReturn("""
                {
                  "leagueSchedule": {
                    "gameDates": [
                      {
                        "games": [
                          {"gameId": "0022600001", "gameDateEst": "2026-10-20T00:00:00Z"},
                          {"gameId": "0022600002", "gameDateEst": "2026-10-20T00:00:00Z"}
                        ]
                      },
                      {
                        "games": [
                          {"gameId": "0022600010", "gameDateEst": "2026-10-21T00:00:00Z"}
                        ]
                      }
                    ]
                  }
                }
                """);
        NbaScheduleService service = new NbaScheduleService(nbaScheduleClient, objectMapper);

        Map<String, List<String>> byDate = service.getGamesByDate();

        assertThat(byDate.get("2026-10-20")).containsExactly("0022600001", "0022600002");
        assertThat(byDate.get("2026-10-21")).containsExactly("0022600010");
    }

    @Test
    void parseGamesByDate_rejectsMissingLeagueSchedule() {
        assertThatThrownBy(() -> NbaScheduleService.parseGamesByDate(objectMapper.readTree("{}")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("leagueSchedule");
    }
}
