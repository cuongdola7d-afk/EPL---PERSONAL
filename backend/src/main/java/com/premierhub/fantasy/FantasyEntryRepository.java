package com.premierhub.fantasy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class FantasyEntryRepository {
    public record Entry(long version, String draftFormation, Instant draftSavedAt, String submittedFormation,
                        Instant submittedAt, Long submittedVersion, Integer submittedTotalOvr) { }
    public record Snapshot(String slotKey, int playerId, String requiredPosition, int clubId,
                           String name, String club, int ovr, String primaryPosition, List<String> eligiblePositions) { }
    private final JdbcTemplate jdbc;
    public FantasyEntryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void lockAccount(long accountId) {
        if (jdbc.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE", Long.class, accountId).isEmpty())
            throw new IllegalStateException("Session account unavailable");
    }
    public Optional<Entry> entry(long accountId, int gameweek, boolean lock) {
        return jdbc.query("SELECT * FROM fantasy_entries WHERE account_id=? AND season=2026 AND gameweek=?" + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> new Entry(rs.getLong("version"), rs.getString("draft_formation"), instant(rs,"draft_saved_at"),
                rs.getString("submitted_formation"), instant(rs,"submitted_at"), rs.getObject("submitted_version", Long.class),
                rs.getObject("submitted_total_ovr", Integer.class)), accountId, gameweek).stream().findFirst();
    }
    public Map<String, Integer> draft(long accountId, int gameweek) {
        var result = new LinkedHashMap<String, Integer>();
        jdbc.query("SELECT slot_key,player_id FROM fantasy_draft_picks WHERE account_id=? AND season=2026 AND gameweek=? ORDER BY slot_key",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> result.put(rs.getString(1),rs.getInt(2)), accountId,gameweek);
        return result;
    }
    public List<Snapshot> submitted(long accountId, int gameweek) {
        return jdbc.query("SELECT * FROM fantasy_submitted_picks WHERE account_id=? AND season=2026 AND gameweek=? ORDER BY slot_key",
                (rs,row) -> new Snapshot(rs.getString("slot_key"),rs.getInt("player_id"),rs.getString("required_position"),
                rs.getInt("club_id"),rs.getString("player_name"),rs.getString("club_name"),rs.getInt("ovr"),
                rs.getString("primary_position"),List.copyOf(Arrays.asList(rs.getString("eligible_positions").split(",")))), accountId,gameweek);
    }
    public void replaceDraft(long accountId, int gameweek, Map<String,Integer> picks) {
        jdbc.update("DELETE FROM fantasy_draft_picks WHERE account_id=? AND season=2026 AND gameweek=?",accountId,gameweek);
        picks.forEach((slot,id) -> jdbc.update("INSERT INTO fantasy_draft_picks VALUES (?,2026,?,?,?)",accountId,gameweek,slot,id));
    }
    public void replaceSubmitted(long accountId, int gameweek, List<Snapshot> snapshots) {
        jdbc.update("DELETE FROM fantasy_submitted_picks WHERE account_id=? AND season=2026 AND gameweek=?",accountId,gameweek);
        for (var s : snapshots) jdbc.update("INSERT INTO fantasy_submitted_picks VALUES (?,2026,?,?,?,?,?,?,?,?,?,?)",
                accountId,gameweek,s.slotKey(),s.playerId(),s.requiredPosition(),s.clubId(),s.name(),s.club(),s.ovr(),
                s.primaryPosition(),String.join(",",s.eligiblePositions()));
    }
    public void write(long accountId, int gameweek, Entry entry, boolean exists) {
        if (!exists) jdbc.update("""
                INSERT INTO fantasy_entries VALUES (?,2026,?,?,?,?,?,?,?,?)
                """,accountId,gameweek,entry.version(),entry.draftFormation(),GameweekRepository.timestamp(entry.draftSavedAt()),
                entry.submittedFormation(),GameweekRepository.timestamp(entry.submittedAt()),entry.submittedVersion(),entry.submittedTotalOvr());
        else jdbc.update("""
                UPDATE fantasy_entries SET version=?,draft_formation=?,draft_saved_at=?,submitted_formation=?,
                    submitted_at=?,submitted_version=?,submitted_total_ovr=? WHERE account_id=? AND season=2026 AND gameweek=?
                """,entry.version(),entry.draftFormation(),GameweekRepository.timestamp(entry.draftSavedAt()),entry.submittedFormation(),
                GameweekRepository.timestamp(entry.submittedAt()),entry.submittedVersion(),entry.submittedTotalOvr(),accountId,gameweek);
    }
    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime().toInstant(ZoneOffset.UTC);
    }
}
