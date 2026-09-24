package com.premierhub.sync;

import com.premierhub.service.FootballQueries;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sync-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class FootballSyncTest {
    @Autowired private FootballSync sync;
    @Autowired private FootballQueries queries;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @MockitoBean private ApiFootballClient api;

    @Test
    void convertsNullableStatisticsAndTransfersAndCanRepeatGameweek() throws Exception {
        when(api.get("/teams?league=39&season=2024", 10)).thenReturn(mapper.readTree("""
                {"response":[
                  {"team":{"id":1,"name":"Arsenal"},"venue":{"city":"London"}},
                  {"team":{"id":2,"name":"Chelsea"},"venue":{"city":"London"}}]}
                """));
        String fixturePath = "/fixtures?league=39&season=2024&round=Regular+Season+-+1";
        when(api.get(fixturePath, 10)).thenReturn(mapper.readTree("""
                {"response":[
                  {"fixture":{"id":100,"date":"2024-08-16T19:00:00+00:00","status":{"short":"FT"}},
                   "teams":{"home":{"id":1},"away":{"id":2}},"goals":{"home":2,"away":1}},
                  {"fixture":{"id":101,"date":"2024-08-17T19:00:00+00:00","status":{"short":"PST"}},
                   "teams":{"home":{"id":2},"away":{"id":1}},"goals":{"home":null,"away":null}}]}
                """));
        when(api.get("/standings?league=39&season=2024", 10)).thenReturn(mapper.readTree("""
                {"response":[{"league":{"standings":[[
                  {"rank":1,"team":{"id":1},"points":3,"goalsDiff":1,
                   "all":{"played":1,"win":1,"draw":0,"lose":0,"goals":{"for":2,"against":1}}},
                  {"rank":2,"team":{"id":2},"points":0,"goalsDiff":-1,
                   "all":{"played":1,"win":0,"draw":0,"lose":1,"goals":{"for":1,"against":2}}}
                ]]}}]}
                """));
        when(api.get("/fixtures/players?fixture=100", 10)).thenReturn(mapper.readTree("""
                {"response":[
                  {"team":{"id":1},"players":[{"player":{"id":11,"name":"Player One"},
                   "statistics":[{"games":{"minutes":90,"position":"F","rating":"7.2"},
                   "goals":{"total":1,"assists":null},"cards":{"yellow":0,"red":0},
                   "shots":{"on":2},"passes":{"key":1},"tackles":{"total":0}}]}]},
                  {"team":{"id":2},"players":[{"player":{"id":12,"name":"Bench Player"},
                   "statistics":[{"games":{"minutes":null,"position":"M","rating":null},
                   "goals":{"total":null,"assists":null},"cards":{"yellow":0,"red":0}}]}]}
                ]}
                """));
        var playerPage = mapper.readTree("""
                {"paging":{"total":1},"response":[{"player":{"id":11,"name":"Player One"},
                  "statistics":[
                   {"league":{"id":39,"season":2024},"team":{"id":1,"name":"Arsenal"},
                    "games":{"appearences":10,"minutes":900,"position":"Attacker"},
                    "goals":{"total":3,"assists":2}},
                   {"league":{"id":39,"season":2024},"team":{"id":2,"name":"Chelsea"},
                    "games":{"appearences":0,"minutes":null,"position":"Attacker"},
                    "goals":{"total":null,"assists":null}}]}]}
                """);
        when(api.get("/players?team=1&season=2024&page=1", 10)).thenReturn(playerPage);
        when(api.get("/players?team=2&season=2024&page=1", 10)).thenReturn(playerPage);

        sync.sync(2024, 1, 10, false, false);
        sync.sync(2024, 1, 10, false, false);

        assertEquals(2, queries.clubs(2024, null).size());
        assertEquals(2, queries.players(2024, null, null).size());
        assertEquals(2, queries.matches(2024, null, null, null).size());
        assertEquals("POSTPONED", queries.match(101, 2024).orElseThrow().status());
        assertNull(queries.match(101, 2024).orElseThrow().homeGoals());
        assertEquals(3, queries.standings(2024, null).getFirst().points());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM fixture_player_stats", Integer.class));
        assertNull(jdbc.queryForObject("SELECT minutes FROM fixture_player_stats WHERE player_id=12", Integer.class));
        assertNull(jdbc.queryForObject("SELECT assists FROM fixture_player_stats WHERE player_id=11", Integer.class));
        verify(api, times(1)).get("/fixtures/players?fixture=100", 10);
        verify(api, times(1)).get("/players?team=1&season=2024&page=1", 10);
        verify(api, times(1)).get("/players?team=2&season=2024&page=1", 10);
    }
}
