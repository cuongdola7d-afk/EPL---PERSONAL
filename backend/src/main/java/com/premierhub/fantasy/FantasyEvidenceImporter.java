package com.premierhub.fantasy;

import com.premierhub.manualstats.ManualMatchStatsCsvReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import static com.premierhub.fantasy.FantasyResultRepository.*;

@Service
public class FantasyEvidenceImporter {
    public record Result(int fixtures,int fixtureChanges,int unratedChanges,int absenceChanges) {}
    private record Confirmation(int fixture,int player,Integer minutes,String reason) {}
    private final FantasyResultRepository repository;
    private final FantasyResultService scoring;
    private final GameweekRepository gameweeks;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public FantasyEvidenceImporter(FantasyResultRepository repository,FantasyResultService scoring,GameweekRepository gameweeks,
            JdbcTemplate jdbc,@Qualifier("fantasyGameweekClock") Clock clock) {
        this.repository=repository;this.scoring=scoring;this.gameweeks=gameweeks;this.jdbc=jdbc;this.clock=clock;
    }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Result importFiles(Path statsFile,Path unratedFile,Path sourceFile) throws IOException {
        return importFiles(statsFile,unratedFile,null,sourceFile);
    }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Result importFiles(Path statsFile,Path unratedFile,Path absenceFile,Path sourceFile) throws IOException {
        if(!Files.isRegularFile(sourceFile) || Files.readString(sourceFile,StandardCharsets.UTF_8).isBlank())
            throw new IllegalArgumentException("A nonempty sources file is required");
        String source=sourceFile.normalize().toString().replace('\\','/');
        if(source.length()>500) throw new IllegalArgumentException("Source reference exceeds 500 characters");
        var rows=statsFile==null ? List.<ManualMatchStatsCsvReader.Row>of() : new ManualMatchStatsCsvReader().read(statsFile);
        var unrated=unratedFile==null ? List.<Confirmation>of() : readUnrated(unratedFile);
        var absences=absenceFile==null ? List.<MatchAbsence>of() : readAbsences(absenceFile,source);
        if(statsFile==null && unratedFile==null && absenceFile==null)
            throw new IllegalArgumentException("Supply statistics, confirmed-unrated or confirmed-not-registered file");
        var fixtureIds=new TreeSet<Integer>();rows.forEach(r->fixtureIds.add(r.fixtureId()));unrated.forEach(r->fixtureIds.add(r.fixture()));
        absences.forEach(r->fixtureIds.add(r.fixtureId()));
        var fixtures=new LinkedHashMap<Integer,Fixture>();fixtureIds.forEach(id->fixtures.put(id,repository.fixture(id)));
        var weeks=new TreeSet<Integer>();fixtures.values().forEach(f->weeks.add(f.gameweek()));
        // No contest need be open to collect evidence; if present, share the publication lock.
        weeks.forEach(gameweeks::lock);
        weeks.forEach(gw->repository.fixtures(gw,true));
        int unratedChanges=0,absenceChanges=0;
        var members=repository.members();
        for(var c:unrated) {
            var stat=repository.stats(c.fixture(),true).stream().filter(s->s.playerId()==c.player()).findFirst()
                    .orElseThrow(()->new IllegalArgumentException("Missing statistics fixture="+c.fixture()+" player="+c.player()));
            if(!"PLAYED".equals(stat.status()) || stat.rating()!=null || c.minutes()!=null && !c.minutes().equals(stat.minutes()))
                throw new IllegalArgumentException("Unrated confirmation conflicts fixture="+c.fixture()+" player="+c.player());
            if(repository.saveUnrated(new Unrated(c.fixture(),c.player(),source,c.reason()),clock.instant())) unratedChanges++;
        }
        for(var absence:absences) {
            var fixture=fixtures.get(absence.fixtureId());
            var active=members.stream().filter(m->m.playerId()==absence.playerId() && m.applies(fixture)).toList();
            if(!"FINISHED".equals(fixture.status()) || active.size()!=1 || active.getFirst().clubId()!=absence.clubId()
                    || repository.stats(fixture.id(),true).stream().anyMatch(s->s.playerId()==absence.playerId())
                    || repository.roles(fixture.id(),true).stream().anyMatch(r->r.playerId()==absence.playerId()))
                throw new IllegalArgumentException("Not-registered evidence conflicts fixture="+fixture.id()+" player="+absence.playerId());
            if(repository.saveAbsence(absence,clock.instant())) absenceChanges++;
        }
        var groups=rows.stream().collect(java.util.stream.Collectors.groupingBy(ManualMatchStatsCsvReader.Row::fixtureId));
        int fixtureChanges=0;
        for(int id:new TreeSet<>(groups.keySet())) {
            var saved=repository.stats(id,true);var incoming=groups.get(id);
            if(saved.size()!=incoming.size()) throw new IllegalArgumentException("Final CSV does not cover the exact saved fixture player set: "+id);
            for(var row:incoming) {
                var stat=saved.stream().filter(s->s.playerId()==row.playerId()).findFirst()
                        .orElseThrow(()->new IllegalArgumentException("CSV/database player mismatch fixture="+id+" player="+row.playerId()));
                if(!row.status().equals(stat.status()) || !Objects.equals(row.rating(),stat.rating()) || !Objects.equals(row.minutes(),stat.minutes())
                        || !Objects.equals(row.goals(),stat.goals()) || !Objects.equals(row.assists(),stat.assists())
                        || !Objects.equals(row.yellowCards(),stat.yellowCards()) || !Objects.equals(row.redCards(),stat.redCards()))
                    throw new IllegalArgumentException("CSV/database field conflict fixture="+id+" player="+row.playerId());
            }
            if(unratedFile!=null) {
                // Explicit final sidecar replaces confirmations for these fully supplied fixtures only.
                var currentKeys=unrated.stream().filter(c->c.fixture()==id).map(Confirmation::player).collect(java.util.stream.Collectors.toSet());
                for(var old:repository.unrated(id,true)) if(!currentKeys.contains(old.playerId())) {
                    jdbc.update("DELETE FROM fantasy_unrated_confirmations WHERE fixture_id=? AND player_id=?",id,old.playerId());unratedChanges++;
                }
            }
            if(absenceFile!=null) {
                var currentKeys=absences.stream().filter(a->a.fixtureId()==id).map(MatchAbsence::playerId).collect(java.util.stream.Collectors.toSet());
                for(var old:repository.absences(id,true)) if(!currentKeys.contains(old.playerId())) {
                    jdbc.update("DELETE FROM fantasy_match_absence_confirmations WHERE fixture_id=? AND player_id=?",id,old.playerId());absenceChanges++;
                }
            }
            var noRating=repository.unrated(id,true);var lineups=repository.lineups(id,true);var roles=repository.roles(id,true);
            var savedAbsences=repository.absences(id,true);
            var issues=scoring.fixtureIssues(fixtures.get(id),saved,noRating,lineups,roles,savedAbsences,members);
            if(!issues.isEmpty()) throw new IllegalArgumentException("Fixture "+id+" incomplete: "+issues);
            String hash=scoring.fixtureHash(fixtures.get(id),saved,noRating,lineups,roles);
            if(repository.confirm(id,hash,source,clock.instant())) fixtureChanges++;
        }
        return new Result(groups.size(),fixtureChanges,unratedChanges,absenceChanges);
    }
    private List<MatchAbsence> readAbsences(Path file,String source) throws IOException {
        if(!file.getFileName().toString().equals("confirmed-not-registered.csv"))
            throw new IllegalArgumentException("Use confirmed-not-registered.csv for verified match absence");
        var lines=Files.readAllLines(file,StandardCharsets.UTF_8);
        if(lines.isEmpty() || !lines.getFirst().replaceFirst("^\uFEFF","").strip().equals("fixture_id,player_id,club_id,reason"))
            throw new IllegalArgumentException("Unsupported confirmed-not-registered header");
        var seen=new HashSet<String>();var result=new ArrayList<MatchAbsence>();
        for(int i=1;i<lines.size();i++) {
            if(lines.get(i).isBlank()) continue;
            var fields=lines.get(i).split(",",-1);
            if(fields.length!=4) throw new IllegalArgumentException("Wrong match absence columns at line "+(i+1));
            int fixture=Integer.parseInt(fields[0].strip()),player=Integer.parseInt(fields[1].strip()),club=Integer.parseInt(fields[2].strip());
            if(fixture<=0 || player<=0 || club<=0 || !seen.add(fixture+":"+player)
                    || !"INJURY_NOT_REGISTERED".equals(fields[3].strip()))
                throw new IllegalArgumentException("Invalid match absence at line "+(i+1));
            result.add(new MatchAbsence(fixture,player,club,"INJURY_NOT_REGISTERED",source));
        }
        return List.copyOf(result);
    }
    private List<Confirmation> readUnrated(Path file) throws IOException {
        if(!file.getFileName().toString().equals("confirmed-unrated.csv"))
            throw new IllegalArgumentException("Use the explicitly confirmed-unrated.csv sidecar, not a pending rating list");
        var lines=Files.readAllLines(file,StandardCharsets.UTF_8);
        if(lines.isEmpty()) throw new IllegalArgumentException("Missing unrated header");
        String header=lines.getFirst().replaceFirst("^\uFEFF","").strip();
        var known=Set.of("fixture_id,player_id,name,minutes,rating,fantasy_points",
                "fixture_id,club,player_id,name,status,minutes",
                "fixture_id,club,player_id,name,status,field,reason");
        if(!known.contains(header)) throw new IllegalArgumentException("Unsupported confirmed-unrated header");
        var columns=Arrays.asList(header.split(","));
        var seen=new HashSet<String>();var result=new ArrayList<Confirmation>();
        for(int i=1;i<lines.size();i++) {
            if(lines.get(i).isBlank()) continue;
            if(lines.get(i).contains("\"") || lines.get(i).contains("\t")) throw new IllegalArgumentException("Quoted/tab unrated fields unsupported at line "+(i+1));
            var fields=lines.get(i).split(",",-1);
            if(fields.length!=columns.size()) throw new IllegalArgumentException("Wrong unrated columns at line "+(i+1));
            int fixture=Integer.parseInt(fields[columns.indexOf("fixture_id")].strip());
            int player=Integer.parseInt(fields[columns.indexOf("player_id")].strip());
            if(fixture<=0 || player<=0 || !seen.add(fixture+":"+player)) throw new IllegalArgumentException("Invalid/repeated unrated key "+fixture+":"+player);
            if(columns.contains("status") && !"PLAYED".equals(fields[columns.indexOf("status")].strip())
                    || columns.contains("field") && !"rating".equals(fields[columns.indexOf("field")].strip())
                    || columns.contains("rating") && !fields[columns.indexOf("rating")].isBlank()
                    || columns.contains("fantasy_points") && !fields[columns.indexOf("fantasy_points")].isBlank())
                throw new IllegalArgumentException("Unrated CSV must explicitly describe PLAYED with no rating");
            Integer minutes=columns.contains("minutes") ? Integer.valueOf(fields[columns.indexOf("minutes")].strip()) : null;
            String reason=columns.contains("reason") ? fields[columns.indexOf("reason")].strip() : "SofaScore không chấm — xác nhận trong file nguồn";
            if(reason.isBlank() || reason.length()>500) throw new IllegalArgumentException("Missing/long unrated reason");
            result.add(new Confirmation(fixture,player,minutes,reason));
        }
        return List.copyOf(result);
    }
}
