package com.premierhub.lineups;

import com.premierhub.web.dto.MatchLineupResponse;
import com.premierhub.web.dto.MatchPlayerStatResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Read-only metadata; never uses minutes, ratings or eligibility to identify starters. */
public final class MatchLineupQueries {
    private final JdbcTemplate jdbc;

    public MatchLineupQueries(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public MatchLineupResponse read(int fixtureId, int clubId, List<MatchPlayerStatResponse> stats) {
        var fixture = jdbc.query("""
                SELECT formation,formation_verified_on,formation_source,roles_verified_on,roles_source
                FROM fixture_lineups WHERE league_id=39 AND season_year=2026 AND fixture_id=? AND club_id=?
                """, (rs, index) -> new Fixture(rs.getString(1), date(rs.getDate(2)), rs.getString(3),
                        date(rs.getDate(4)), rs.getString(5)), fixtureId, clubId).stream().findFirst().orElse(null);
        var defaults = jdbc.query("""
                SELECT default_formation,updated_on,scope_from_gw,scope_to_gw,verified_matches,formation_counts,fixture_ids,source_note
                FROM club_season_formations WHERE league_id=39 AND season_year=2026 AND club_id=?
                """, (rs, index) -> new Default(rs.getString(1), date(rs.getDate(2)), rs.getInt(3), rs.getInt(4),
                        rs.getInt(5), rs.getString(6), rs.getString(7), rs.getString(8)), clubId).stream().findFirst().orElse(null);
        List<MatchLineupResponse.Player> players = jdbc.query("""
                SELECT l.player_id,l.role,l.match_position,l.row_index,l.slot_index,
                    l.substitution_in_minute,l.substitution_out_minute,p.primary_position
                FROM fixture_lineup_players l
                LEFT JOIN player_specific_positions p ON p.player_id=l.player_id
                    AND p.league_id=39 AND p.season_year=2026
                WHERE l.fixture_id=? AND l.club_id=? ORDER BY l.player_id
                """, (rs, index) -> new MatchLineupResponse.Player(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getObject(4, Integer.class), rs.getObject(5, Integer.class),
                        rs.getObject(6, Integer.class), rs.getObject(7, Integer.class), rs.getString(8)), fixtureId, clubId);
        Set<Integer> sourceIds = players.stream().map(MatchLineupResponse.Player::playerId).collect(Collectors.toSet());
        Set<Integer> statIds = stats.stream().map(MatchPlayerStatResponse::playerId).collect(Collectors.toSet());
        var byPlayer = stats.stream().collect(Collectors.toMap(MatchPlayerStatResponse::playerId, player -> player));
        boolean verified = fixture != null && fixture.rolesDate() != null && sourceIds.size() == players.size()
                && sourceIds.equals(statIds) && players.stream().filter(player -> player.role().equals("STARTER")).count() == 11
                && players.stream().allMatch(player -> Set.of("STARTER", "SUB_USED", "SUB_UNUSED").contains(player.role())
                    && (player.role().equals("SUB_UNUSED") ? "DID_NOT_PLAY" : "PLAYED")
                        .equals(byPlayer.get(player.playerId()).participationStatus()));
        boolean actual = fixture != null && fixture.formation() != null && fixture.formationDate() != null && fixture.formationSource() != null;
        String formation = actual ? fixture.formation() : defaults == null ? null : defaults.formation();
        if (formation != null) Formation.lines(formation);
        return new MatchLineupResponse(formation, formation == null ? "MISSING" : actual ? "FIXTURE" : "CLUB_DEFAULT",
                actual ? fixture.formationDate() : defaults == null ? null : defaults.updatedOn(),
                actual ? fixture.formationSource() : defaults == null ? null : defaults.sourceNote(),
                defaults == null ? null : defaults.from(), defaults == null ? null : defaults.to(),
                defaults == null ? null : defaults.count(), defaults == null ? null : defaults.frequencies(),
                defaults == null ? null : defaults.ids(), verified ? "VERIFIED" : players.isEmpty() ? "MISSING" : "INCOMPLETE",
                fixture == null ? null : fixture.rolesDate(), fixture == null ? null : fixture.rolesSource(), players);
    }

    private static LocalDate date(java.sql.Date value) { return value == null ? null : value.toLocalDate(); }
    private record Fixture(String formation, LocalDate formationDate, String formationSource,
                           LocalDate rolesDate, String rolesSource) { }
    private record Default(String formation, LocalDate updatedOn, int from, int to, int count,
                           String frequencies, String ids, String sourceNote) { }
}
