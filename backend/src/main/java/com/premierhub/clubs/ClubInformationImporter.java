package com.premierhub.clubs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
public class ClubInformationImporter {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public ClubInformationImporter(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    public Result importFile(Path file) throws IOException {
        var rows = new ClubInformationCsvReader().read(file);
        return transactions.execute(status -> importRows(rows));
    }

    private Result importRows(List<ClubInformationCsvReader.Row> rows) {
        // Lock the existing season keys before validating, including concurrent importer runs.
        var clubs = new HashSet<>(jdbc.queryForList("""
                SELECT club_id FROM season_clubs WHERE league_id=39 AND season_year=2026
                ORDER BY club_id FOR UPDATE
                """, Integer.class));
        var inserts = new ArrayList<Object[]>();
        for (var row : rows) {
            if (!clubs.contains(row.clubId())) throw invalid(row, "club_id is not in the 2026/27 roster");
            var saved = jdbc.query("""
                    SELECT manager_name,manager_status,stadium_name,verified_on
                    FROM club_season_information WHERE league_id=39 AND season_year=2026 AND club_id=?
                    """, (rs, index) -> new Saved(rs.getString(1), rs.getString(2), rs.getString(3), rs.getDate(4).toLocalDate()), row.clubId());
            var incoming = new Saved(row.managerName(), row.managerStatus(), row.stadiumName(), row.verifiedOn());
            if (!saved.isEmpty()) {
                Saved current = saved.getFirst();
                conflict(row, "manager_name", current.manager(), incoming.manager());
                conflict(row, "manager_status", current.status(), incoming.status());
                conflict(row, "stadium_name", current.stadium(), incoming.stadium());
                conflict(row, "verified_on", current.date(), incoming.date());
            } else {
                inserts.add(new Object[]{39, 2026, row.clubId(), row.managerName(), row.managerStatus(),
                        row.stadiumName(), Date.valueOf(row.verifiedOn())});
            }
        }
        if (!inserts.isEmpty()) jdbc.batchUpdate("""
                INSERT INTO club_season_information
                (league_id,season_year,club_id,manager_name,manager_status,stadium_name,verified_on)
                VALUES (?,?,?,?,?,?,?)
                """, inserts);
        return new Result(rows.size(), inserts.size());
    }

    private static void conflict(ClubInformationCsvReader.Row row, String field, Object saved, Object incoming) {
        if (!Objects.equals(saved, incoming)) throw invalid(row, "conflict " + field + ": saved=" + saved + ", incoming=" + incoming);
    }

    private static IllegalArgumentException invalid(ClubInformationCsvReader.Row row, String message) {
        return new IllegalArgumentException("CSV line " + row.line() + " club_id=" + row.clubId() + ": " + message);
    }

    private record Saved(String manager, String status, String stadium, LocalDate date) { }
    public record Result(int rows, int inserted) { }
}
