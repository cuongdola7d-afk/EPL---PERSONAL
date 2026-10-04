package com.premierhub.fantasy;

import com.premierhub.service.FantasyLineupService;
import com.premierhub.web.dto.FantasyLineupRequest;
import com.premierhub.web.dto.FantasyValidationResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static com.premierhub.fantasy.FantasyEntryRepository.*;

@Service
public class FantasyEntryService {
    public record Draft(String formation, Map<String,Integer> picks, Instant savedAt) { }
    public record Submitted(String formation, Map<String,Integer> picks, int totalOvr, Instant submittedAt,
                            long version, List<Snapshot> players) { }
    public record Mine(long accountId, int season, int gameweek, long version, Draft draft, Submitted submitted,
                       Instant serverTimeUtc) { }
    public static class InvalidLineup extends RuntimeException {
        private final FantasyValidationResponse validation;
        InvalidLineup(FantasyValidationResponse validation) { this.validation = validation; }
        public FantasyValidationResponse validation() { return validation; }
    }
    private final FantasyEntryRepository entries;
    private final GameweekRepository gameweeks;
    private final GameweekService deadlines;
    private final FantasyLineupService validator;
    private final Clock clock;
    public FantasyEntryService(FantasyEntryRepository entries, GameweekRepository gameweeks, GameweekService deadlines,
                               FantasyLineupService validator, @Qualifier("fantasyGameweekClock") Clock clock) {
        this.entries=entries; this.gameweeks=gameweeks; this.deadlines=deadlines; this.validator=validator; this.clock=clock;
    }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public Mine read(long accountId, int gameweek) {
        validateGameweek(gameweek);
        var entry=entries.entry(accountId,gameweek,false).orElse(null);
        if (entry==null) return new Mine(accountId,2026,gameweek,0,null,null,clock.instant());
        return response(accountId,gameweek,entry,entries.draft(accountId,gameweek),entries.submitted(accountId,gameweek),clock.instant());
    }
    @Transactional
    public Mine save(long accountId, int gameweek, String formation, Map<String,Integer> picks, long expectedVersion, boolean submit) {
        validateGameweek(gameweek);
        // One lock order everywhere: GW -> account (also protects first creation) -> entry.
        var configuration=gameweeks.lock(gameweek).orElseThrow(() -> conflict("GW chưa công bố deadline."));
        entries.lockAccount(accountId);
        var previous=entries.entry(accountId,gameweek,true).orElse(null);
        deadlines.requireOpen(configuration); // time AFTER all locks, including a queued request
        long version=previous==null ? 0 : previous.version();
        if (version!=expectedVersion) throw conflict("Dữ liệu đã thay đổi ở tab hoặc thiết bị khác. Tải lại trước khi lưu/chốt.");
        var inspected=validator.inspect(new FantasyLineupRequest(formation,picks),submit);
        if (!inspected.validation().valid()) throw new InvalidLineup(inspected.validation());
        var snapshots=submit ? picks.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(pick -> {
            var player=inspected.players().get(pick.getValue());
            return new Snapshot(pick.getKey(),player.id(),inspected.slots().get(pick.getKey()),player.clubId(),
                    player.name(),player.club(),player.fc27Overall(),player.primaryPosition(),List.copyOf(player.eligiblePositions()));
        }).toList() : entries.submitted(accountId,gameweek);
        Instant now=deadlines.requireOpen(configuration).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        // immediately before writes; slow validation cannot bypass deadline
        var next=new Entry(version+1,formation,now,
                submit ? formation : previous==null ? null : previous.submittedFormation(),
                submit ? now : previous==null ? null : previous.submittedAt(),
                submit ? Long.valueOf(version+1) : previous==null ? null : previous.submittedVersion(),
                submit ? Integer.valueOf(inspected.validation().totalOvr()) : previous==null ? null : previous.submittedTotalOvr());
        entries.write(accountId,gameweek,next,previous!=null);
        entries.replaceDraft(accountId,gameweek,picks);
        if (submit) entries.replaceSubmitted(accountId,gameweek,snapshots);
        // If SQL waits cross the deadline, rollback ALL changes, including deletion of the old snapshot.
        Instant accepted=deadlines.requireOpen(configuration);
        return response(accountId,gameweek,next,picks,snapshots,accepted); // no more SQL before transaction commit
    }
    private static Mine response(long accountId,int gameweek,Entry entry,Map<String,Integer> picks,List<Snapshot> snapshots,Instant now) {
        var submittedPicks=new java.util.LinkedHashMap<String,Integer>();
        snapshots.forEach(s -> submittedPicks.put(s.slotKey(),s.playerId()));
        return new Mine(accountId,2026,gameweek,entry.version(),entry.draftFormation()==null ? null :
                new Draft(entry.draftFormation(),Map.copyOf(picks),entry.draftSavedAt()),
                entry.submittedAt()==null ? null : new Submitted(entry.submittedFormation(),submittedPicks,entry.submittedTotalOvr(),
                        entry.submittedAt(),entry.submittedVersion(),snapshots),now);
    }
    private static void validateGameweek(int gameweek) {
        if (gameweek<6 || gameweek>38) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chỉ lưu/chốt cuộc thi chính thức GW6–38 mùa 2026/27.");
    }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
