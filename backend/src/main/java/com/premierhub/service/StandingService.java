package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.model.Standing;
import com.premierhub.repository.MatchRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StandingService {
    private final List<RankedStanding> table;

    public StandingService(LeagueTableService calculator, MatchRepository matches,
                           List<Club> clubs) {
        List<Standing> standings = calculator.calculate(clubs, matches.findAll());
        List<RankedStanding> ranked = new ArrayList<>();
        for (int index = 0; index < standings.size(); index++) {
            ranked.add(new RankedStanding(index + 1, standings.get(index)));
        }
        this.table = List.copyOf(ranked);
    }

    public List<RankedStanding> getStandings(Integer limit) {
        if (limit != null && limit < 1) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        if (limit == null || limit >= table.size()) {
            return table;
        }
        return table.subList(0, limit);
    }

    public Optional<RankedStanding> findByClubId(int clubId) {
        return table.stream()
                .filter(entry -> entry.standing().getClub().getId() == clubId)
                .findFirst();
    }

    public record RankedStanding(int position, Standing standing) {
    }
}
