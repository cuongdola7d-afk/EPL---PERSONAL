package com.premierhub.service;

import com.premierhub.repository.TeamOfWeekRepository;
import com.premierhub.repository.TeamOfWeekRepository.Appearance;
import com.premierhub.web.dto.TeamOfWeekResponse;
import com.premierhub.web.dto.TeamOfWeekResponse.Conflict;
import com.premierhub.web.dto.TeamOfWeekResponse.Pick;
import com.premierhub.web.dto.TeamOfWeekResponse.Player;
import com.premierhub.web.error.InvalidFilterException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamOfWeekService {
    private final TeamOfWeekRepository repository;

    public TeamOfWeekService(TeamOfWeekRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public TeamOfWeekResponse team(int gameweek) {
        if (gameweek < 1 || gameweek > 5) throw new InvalidFilterException("Gameweek must be between 1 and 5");
        var pool = repository.read(gameweek);
        var played = pool.appearances();
        var rated = played.stream().filter(row -> row.rating() != null).toList();
        var candidates = rated.stream().filter(row -> !row.eligiblePositions().isEmpty()).toList();
        var byPlayer = new TreeMap<Integer, List<Appearance>>();
        played.forEach(row -> byPlayer.computeIfAbsent(row.playerId(), ignored -> new ArrayList<>()).add(row));
        var conflicts = byPlayer.values().stream().filter(rows -> rows.size() > 1)
                .map(rows -> new Conflict(rows.getFirst().playerId(), rows.getFirst().name(),
                        rows.stream().map(Appearance::fixtureId).sorted().toList())).toList();
        var picks = new ArrayList<Pick>();
        var missing = new ArrayList<String>();
        String status;
        BigDecimal total = null;
        if (!conflicts.isEmpty()) {
            // Do not invent a max/average/sum rule for multiple appearances in one gameweek.
            status = "MULTIPLE_MATCHES";
            TeamOfWeekOptimizer.SLOTS.forEach(slot -> { picks.add(new Pick(slot, null)); missing.add(slot); });
        } else {
            var selection = TeamOfWeekOptimizer.select(candidates);
            status = selection.filledSlots() == 11 ? "COMPLETE" : "INSUFFICIENT_DATA";
            if (selection.filledSlots() == 11) total = BigDecimal.valueOf(selection.ratingCents(), 2);
            for (int slot = 0; slot < TeamOfWeekOptimizer.SLOTS.size(); slot++) {
                var appearance = selection.players().get(slot);
                String name = TeamOfWeekOptimizer.SLOTS.get(slot);
                picks.add(new Pick(name, appearance == null ? null : new Player(appearance.playerId(),
                        appearance.name(), appearance.clubId(), appearance.club(), appearance.fixtureId(),
                        appearance.rating(), appearance.eligiblePositions())));
                if (appearance == null) missing.add(name);
            }
        }
        return new TeamOfWeekResponse(2026, gameweek, "4-3-3", status, total, played.size(), rated.size(),
                candidates.size(), (int) played.stream().filter(row -> row.eligiblePositions().isEmpty()).count(),
                played.size() - rated.size(),
                pool.completedFixtures(), pool.recordedFixtures(), List.copyOf(picks), List.copyOf(missing), conflicts);
    }
}
