package com.premierhub.fantasy;

import com.premierhub.service.FantasyLineupService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import static com.premierhub.fantasy.FantasyResultRepository.*;

@Service
public class FantasyResultService {
    public record Issue(Integer fixtureId,Integer playerId,Long accountId,String code,String message) {}
    public record MatchScore(int fixtureId,int clubId,BigDecimal rating,BigDecimal points,String reason) {}
    public record PlayerScore(String slotKey,int playerId,String name,String club,String position,
                              BigDecimal points,List<MatchScore> matches) {}
    public record TeamScore(String formation,BigDecimal totalPoints,List<PlayerScore> players) {}
    public record Readiness(int season,int gameweek,boolean ready,int fixtures,int participants,int currentVersion,
                            List<Issue> issues,List<Publication> history) {}
    public record Published(int season,int gameweek,int version,int participants,Instant publishedAt,boolean unchanged) {}
    public record Mine(long accountId,int season,int gameweek,String status,Integer version,Instant publishedAt,TeamScore result) {}
    public record Ranked(int rank,long accountId,String displayName,BigDecimal totalPoints,int gameweeksPlayed) {}
    public record Leaderboard(int season,Integer gameweek,String status,Integer version,int publishedGameweeks,List<Ranked> players) {}
    private record Source(Fixture fixture,List<Stat> stats,List<Unrated> unrated,List<Lineup> lineups,List<Role> roles) {}
    private record Submitted(Participant participant,List<FantasyEntryRepository.Snapshot> players) {}
    private record Plan(Readiness readiness,String hash,List<Participant> participants,Map<Long,TeamScore> scores) {}
    public static class NotReady extends RuntimeException {
        private final Readiness readiness;
        NotReady(Readiness readiness) { super("GW chưa đủ điều kiện công bố.");this.readiness=readiness; }
        public Readiness readiness() { return readiness; }
    }
    private final FantasyResultRepository repository;
    private final FantasyEntryRepository entries;
    private final GameweekRepository gameweeks;
    private final Clock clock;
    private final ObjectMapper json;
    public FantasyResultService(FantasyResultRepository repository,FantasyEntryRepository entries,GameweekRepository gameweeks,
            @Qualifier("fantasyGameweekClock") Clock clock,ObjectMapper json) {
        this.repository=repository;this.entries=entries;this.gameweeks=gameweeks;this.clock=clock;this.json=json;
    }
    public String hash(Object data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(data))); }
        catch(java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public String fixtureHash(Fixture f,List<Stat> stats,List<Unrated> unrated,List<Lineup> lineups,List<Role> roles) {
        return hash(new Source(f,stats,unrated,lineups,roles));
    }
    static boolean present(String text) { return text!=null && !text.isBlank(); }
    static void issue(List<Issue> issues,Integer fixture,Integer player,Long account,String code,String message) {
        issues.add(new Issue(fixture,player,account,code,message));
    }
    public List<Issue> fixtureIssues(Fixture f,List<Stat> stats,List<Unrated> unrated,List<Lineup> lineups,List<Role> roles,List<Member> members) {
        var issues=new ArrayList<Issue>();
        if(!"FINISHED".equals(f.status())) issue(issues,f.id(),null,null,"FIXTURE_PENDING","Trận chưa FINISHED: "+f.status());
        var byPlayer=new HashMap<Integer,Stat>();stats.forEach(s->byPlayer.put(s.playerId(),s));
        var noRating=new HashSet<Integer>();unrated.forEach(r->noRating.add(r.playerId()));
        for(int club:List.of(f.home(),f.away())) {
            var lineup=lineups.stream().filter(l->l.clubId()==club).findFirst();
            if(lineup.isEmpty() || lineup.get().rolesVerified()==null || !present(lineup.get().rolesSource())
                    || lineup.get().formationVerified()==null || !present(lineup.get().formation()) || !present(lineup.get().formationSource()))
                issue(issues,f.id(),null,null,"LINEUP_UNCONFIRMED","Chưa xác nhận đầy đủ đội hình/sơ đồ CLB #"+club);
            long starters=roles.stream().filter(r->r.clubId()==club && "STARTER".equals(r.role())).count();
            if(starters!=11) issue(issues,f.id(),null,null,"STARTERS_UNCONFIRMED","CLB #"+club+" có "+starters+" người được xác nhận đá chính.");
        }
        for(var role:roles) {
            var stat=byPlayer.get(role.playerId());
            if(stat==null) issue(issues,f.id(),role.playerId(),null,"STAT_MISSING","Danh sách trận đã xác nhận nhưng thiếu dòng thống kê.");
            else if(stat.clubId()!=role.clubId() || !("SUB_UNUSED".equals(role.role()) ? "DID_NOT_PLAY" : "PLAYED").equals(stat.status()))
                issue(issues,f.id(),role.playerId(),null,"ROLE_CONFLICT","Vai trò trong trận và trạng thái thống kê không khớp.");
        }
        for(var stat:stats) {
            if(stat.clubId()!=f.home() && stat.clubId()!=f.away())
                issue(issues,f.id(),stat.playerId(),null,"CLUB_CONFLICT","CLB thống kê không thuộc fixture.");
            var active=members.stream().filter(m->m.playerId()==stat.playerId() && m.applies(f)).toList();
            if(active.size()!=1 || active.getFirst().clubId()!=stat.clubId())
                issue(issues,f.id(),stat.playerId(),null,"MEMBERSHIP","Thiếu hoặc xung đột membership tại ngày trận.");
            if("DID_NOT_PLAY".equals(stat.status())) {
                if(stat.rating()!=null || stat.minutes()==null || stat.minutes()!=0
                        || positive(stat.goals()) || positive(stat.assists()) || positive(stat.yellowCards()) || positive(stat.redCards()))
                    issue(issues,f.id(),stat.playerId(),null,"DNP_INVALID","DID_NOT_PLAY chưa có dữ liệu hợp lệ.");
            } else if("PLAYED".equals(stat.status())) {
                if(stat.minutes()==null || stat.minutes()<=0 || stat.goals()==null || stat.assists()==null
                        || stat.yellowCards()==null || stat.redCards()==null)
                    issue(issues,f.id(),stat.playerId(),null,"STATS_INCOMPLETE","PLAYED thiếu phút/bàn/kiến tạo/thẻ bắt buộc.");
                if(stat.rating()==null && !noRating.contains(stat.playerId()))
                    issue(issues,f.id(),stat.playerId(),null,"RATING_PENDING","Rating chưa thu thập; chưa có xác nhận SofaScore không chấm.");
                if(stat.rating()!=null && (stat.rating().signum()<0 || stat.rating().compareTo(BigDecimal.TEN)>0))
                    issue(issues,f.id(),stat.playerId(),null,"RATING_INVALID","Rating ngoài phạm vi 0–10.");
                if(roles.stream().noneMatch(r->r.playerId()==stat.playerId() && r.clubId()==stat.clubId() && !"SUB_UNUSED".equals(r.role())))
                    issue(issues,f.id(),stat.playerId(),null,"ROLE_MISSING","PLAYED thiếu xác nhận đá chính hoặc vào thay.");
            } else issue(issues,f.id(),stat.playerId(),null,"PARTICIPATION_UNKNOWN","Trạng thái tham gia chưa xác nhận.");
        }
        for(var confirmation:unrated) {
            var stat=byPlayer.get(confirmation.playerId());
            if(stat==null || !"PLAYED".equals(stat.status()) || stat.rating()!=null)
                issue(issues,f.id(),confirmation.playerId(),null,"UNRATED_CONFLICT","Xác nhận không chấm không khớp thống kê hiện tại.");
        }
        return issues;
    }
    private static boolean positive(Integer n) { return n!=null && n>0; }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Readiness readiness(int gw) { official(gw);return plan(gw,gameweeks.find(gw).orElse(null),false).readiness(); }
    private Plan plan(int gw,GameweekRepository.Configuration configuration,boolean lock) {
        var issues=new ArrayList<Issue>();
        var fixtures=repository.fixtures(gw,lock);
        var participants=repository.participants(gw,lock);
        var members=repository.members();
        var sources=new ArrayList<Source>();
        var confirmations=new ArrayList<Evidence>();
        if(configuration==null) issue(issues,null,null,null,"GW_NOT_OPENED","GW chưa được công bố cuộc thi.");
        if(fixtures.isEmpty()) issue(issues,null,null,null,"FIXTURES_MISSING","Chưa có danh sách fixture của GW.");
        for(var f:fixtures) {
            var stats=repository.stats(f.id(),lock);var unrated=repository.unrated(f.id(),lock);
            var lineups=repository.lineups(f.id(),lock);var roles=repository.roles(f.id(),lock);
            sources.add(new Source(f,stats,unrated,lineups,roles));
            issues.addAll(fixtureIssues(f,stats,unrated,lineups,roles,members));
            var confirmation=repository.evidence(f.id(),lock);
            if(confirmation.isEmpty()) issue(issues,f.id(),null,null,"FIXTURE_UNCONFIRMED","Chưa xác nhận CSV cuối/danh sách đầy đủ của cả hai đội.");
            else {
                confirmations.add(confirmation.get());
                if(!confirmation.get().sourceHash().equals(fixtureHash(f,stats,unrated,lineups,roles)))
                    issue(issues,f.id(),null,null,"SOURCE_CHANGED","Thống kê/đội hình đã đổi sau xác nhận; cần xác nhận lại file cuối.");
            }
        }
        var submitted=new ArrayList<Submitted>();var scores=new LinkedHashMap<Long,TeamScore>();
        for(var participant:participants) {
            var picks=entries.submitted(participant.accountId(),gw);
            submitted.add(new Submitted(participant,picks));
            var slots=FantasyLineupService.normalizedSlots(participant.formation());
            boolean valid=picks.size()==11 && slots.size()==11 && picks.stream().map(FantasyEntryRepository.Snapshot::playerId).distinct().count()==11
                    && picks.stream().map(FantasyEntryRepository.Snapshot::slotKey).distinct().count()==11
                    && picks.stream().allMatch(p->Objects.equals(slots.get(p.slotKey()),p.requiredPosition()) && p.eligiblePositions().contains(p.requiredPosition())
                        && p.ovr()>=1 && p.ovr()<=99)
                    && picks.stream().mapToInt(FantasyEntryRepository.Snapshot::ovr).sum()<=860
                    && picks.stream().collect(java.util.stream.Collectors.groupingBy(FantasyEntryRepository.Snapshot::clubId,java.util.stream.Collectors.counting()))
                        .values().stream().allMatch(n->n<=3)
                    && configuration!=null && participant.submittedAt().isBefore(configuration.deadlineUtc());
            if(!valid) { issue(issues,null,null,participant.accountId(),"SUBMISSION_INVALID","Snapshot đội chốt không hợp lệ hoặc chốt sau deadline.");continue; }
            var playerScores=new ArrayList<PlayerScore>();
            for(var pick:picks) {
                var matches=new ArrayList<MatchScore>();
                for(var source:sources) {
                    var f=source.fixture();
                    var stat=source.stats().stream().filter(s->s.playerId()==pick.playerId()).findFirst();
                    boolean expected=stat.isPresent() || source.roles().stream().anyMatch(r->r.playerId()==pick.playerId())
                            || members.stream().anyMatch(m->m.playerId()==pick.playerId() && m.applies(f));
                    if(!expected) continue;
                    if(stat.isEmpty()) { issue(issues,f.id(),pick.playerId(),participant.accountId(),"SELECTED_STAT_MISSING","Cầu thủ được chọn thiếu dòng thống kê; không tự coi là không ra sân.");continue; }
                    var row=stat.get();String reason;
                    BigDecimal points;
                    if("DID_NOT_PLAY".equals(row.status())) { points=BigDecimal.ZERO;reason="DID_NOT_PLAY"; }
                    else if(row.rating()!=null) { points=row.rating();reason="SOFASCORE_RATING"; }
                    else if(source.unrated().stream().anyMatch(u->u.playerId()==pick.playerId())) { points=BigDecimal.ZERO;reason="SOFASCORE_UNRATED_CONFIRMED"; }
                    else continue; // Pending NULL has already blocked readiness; never gets a zero.
                    matches.add(new MatchScore(f.id(),row.clubId(),row.rating(),points.setScale(2),reason));
                }
                if(matches.isEmpty()) issue(issues,null,pick.playerId(),participant.accountId(),"PLAYER_MATCH_UNRESOLVED","Chưa có trận và căn cứ điểm của cầu thủ được chọn trong GW.");
                BigDecimal total=matches.stream().map(MatchScore::points).reduce(new BigDecimal("0.00"),BigDecimal::add);
                playerScores.add(new PlayerScore(pick.slotKey(),pick.playerId(),pick.name(),pick.club(),pick.requiredPosition(),total,List.copyOf(matches)));
            }
            scores.put(participant.accountId(),new TeamScore(participant.formation(),playerScores.stream().map(PlayerScore::points)
                    .reduce(new BigDecimal("0.00"),BigDecimal::add),List.copyOf(playerScores)));
        }
        if(configuration!=null && clock.instant().isBefore(configuration.deadlineUtc()))
            issue(issues,null,null,null,"DEADLINE_PENDING","Chưa tới deadline của GW.");
        var latest=repository.latest(gw);
        var readiness=new Readiness(2026,gw,issues.isEmpty(),fixtures.size(),participants.size(),latest.map(Publication::version).orElse(0),
                List.copyOf(new LinkedHashSet<>(issues)),repository.history(gw));
        return new Plan(readiness,hash(List.of(sources,confirmations,submitted)),participants,scores);
    }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Published publish(int gw,int expectedVersion,long actor,String reason,boolean recalculate) {
        official(gw);
        if(reason==null || reason.strip().length()<3 || reason.strip().length()>500 || reason.chars().anyMatch(Character::isISOControl))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cần lý do công bố/tái tính từ 3 đến 500 ký tự.");
        var config=gameweeks.lock(gw).orElse(null);
        var plan=plan(gw,config,true);
        if(!plan.readiness().ready()) throw new NotReady(plan.readiness());
        var previous=repository.latest(gw);
        if(previous.isPresent() && previous.get().sourceHash().equals(plan.hash()))
            return new Published(2026,gw,previous.get().version(),plan.participants().size(),previous.get().publishedAt(),true);
        int version=previous.map(Publication::version).orElse(0);
        if(version!=expectedVersion || (version>0 && !recalculate) || (version==0 && recalculate))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Phiên bản kết quả đã đổi hoặc cần dùng đúng thao tác công bố/tái tính.");
        Instant now=clock.instant();int next=version+1;
        repository.publication(gw,next,plan.hash(),now,actor,recalculate?"RECALCULATE":"PUBLISH",reason.strip());
        for(var participant:plan.participants()) repository.team(gw,next,participant,plan.scores().get(participant.accountId()));
        repository.markPublished(gw,now); // Last write; failure anywhere rolls back every team's version.
        return new Published(2026,gw,next,plan.participants().size(),now,false);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Mine mine(long owner,int gw) {
        official(gw);
        var entry=entries.entry(owner,gw,false);
        if(entry.isEmpty() || entry.get().submittedAt()==null) return new Mine(owner,2026,gw,"NOT_PARTICIPATING",null,null,null);
        var config=gameweeks.find(gw);
        if(config.isEmpty() || !"PUBLISHED".equals(config.get().workflowStatus())) return new Mine(owner,2026,gw,"AWAITING_RESULTS",null,null,null);
        var publication=repository.latest(gw).orElseThrow(()->new IllegalStateException("Published GW has no result version"));
        var score=repository.team(owner,gw,publication.version()).orElseThrow(()->new IllegalStateException("Published participant has no result"));
        return new Mine(owner,2026,gw,"PUBLISHED",publication.version(),publication.publishedAt(),score);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Leaderboard leaderboard(Integer gw) {
        if(gw!=null) official(gw);
        var configuration=gw==null ? Optional.<GameweekRepository.Configuration>empty() : gameweeks.find(gw);
        var publication=gw==null ? Optional.<Publication>empty() : repository.latest(gw);
        boolean published=gw==null ? repository.publishedGameweeks()>0 : configuration.isPresent()
                && "PUBLISHED".equals(configuration.get().workflowStatus()) && configuration.get().resultsPublishedAt()!=null && publication.isPresent();
        int publishedWeeks=gw==null ? repository.publishedGameweeks() : published?1:0;
        if(!published) return new Leaderboard(2026,gw,"AWAITING_RESULTS",null,publishedWeeks,List.of());
        var ranked=new ArrayList<Ranked>();BigDecimal previous=null;int rank=0;
        for(var standing:repository.standings(gw)) {
            if(previous==null || previous.compareTo(standing.totalPoints())!=0) rank=ranked.size()+1;
            ranked.add(new Ranked(rank,standing.accountId(),standing.displayName(),standing.totalPoints(),standing.gameweeksPlayed()));
            previous=standing.totalPoints();
        }
        return new Leaderboard(2026,gw,"PUBLISHED",publication.map(Publication::version).orElse(null),publishedWeeks,List.copyOf(ranked));
    }
    static void official(int gw) {
        if(gw<6 || gw>38) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chỉ tính cuộc thi chính thức GW6–38 mùa 2026/27.");
    }
}
