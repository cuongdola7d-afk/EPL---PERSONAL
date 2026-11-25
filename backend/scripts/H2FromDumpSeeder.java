import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Seeds a fresh local H2 file database directly from a MySQL mysqldump file without connecting to Aiven.
 */
public class H2FromDumpSeeder {
    private static final List<String> SCHEMA_FILES = List.of(
            "schema.sql",
            "auth-schema.sql",
            "fantasy-gameweek-schema.sql",
            "fantasy-entry-schema.sql",
            "fantasy-result-schema.sql",
            "player-guess-schema.sql"
    );

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: H2FromDumpSeeder <dumpSql> <h2BasePath> <resourcesDir>");
        }
        Path dumpFile = Path.of(args[0]).toAbsolutePath();
        Path h2Base = Path.of(args[1]).toAbsolutePath();
        Path resourcesDir = Path.of(args[2]).toAbsolutePath();

        Files.createDirectories(h2Base.getParent());
        Files.deleteIfExists(Path.of(h2Base + ".mv.db"));
        Files.deleteIfExists(Path.of(h2Base + ".trace.db"));

        String baseUrl = "jdbc:h2:file:" + h2Base.toString().replace("\\", "/") + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
        try (Connection conn = DriverManager.getConnection(baseUrl, "sa", "")) {
            conn.setAutoCommit(false);
            for (String schemaFile : SCHEMA_FILES) {
                String sql = Files.readString(resourcesDir.resolve(schemaFile), StandardCharsets.UTF_8);
                try (Statement st = conn.createStatement()) {
                    st.execute(sql);
                }
            }
            conn.commit();

            Set<String> h2Tables = new HashSet<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT LOWER(TABLE_NAME) FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_SCHEMA) = 'public'")) {
                while (rs.next()) {
                    h2Tables.add(rs.getString(1));
                }
            }

            try (Statement st = conn.createStatement()) {
                st.execute("SET REFERENTIAL_INTEGRITY FALSE");
                for (String table : h2Tables) {
                    st.execute("TRUNCATE TABLE " + table);
                }
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    Files.newInputStream(dumpFile),
                    StandardCharsets.UTF_8.newDecoder()
                            .onMalformedInput(CodingErrorAction.REPLACE)
                            .onUnmappableCharacter(CodingErrorAction.REPLACE)));
                 Statement st = conn.createStatement()) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith("INSERT INTO `")) {
                        continue;
                    }
                    int endTick = line.indexOf('`', 13);
                    if (endTick < 0) {
                        continue;
                    }
                    String table = line.substring(13, endTick).toLowerCase(Locale.ROOT);
                    if (!h2Tables.contains(table) || table.startsWith("spring_session")) {
                        continue;
                    }
                    st.execute(mysqlInsertToH2(line));
                }
            }

            // Ensure reserved system account exists even when seeding from an older dump.
            if (h2Tables.contains("accounts")) {
                try (Statement st = conn.createStatement()) {
                    st.execute("""
                            INSERT INTO accounts (email, display_name, password_hash, role, created_at)
                            SELECT 'system@prismaxi.invalid', 'Hệ thống', NULL, 'ADMIN', CURRENT_TIMESTAMP
                            WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE email = 'system@prismaxi.invalid')
                            """);
                }
            }

            try (Statement st = conn.createStatement()) {
                st.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
            conn.commit();

            String appJdbcUrl = baseUrl + ";AUTO_SERVER=TRUE";
            System.out.println("H2_OK jdbcUrl=" + appJdbcUrl + " tables=" + h2Tables.size() + " source=" + dumpFile);
        }
    }

    static String mysqlInsertToH2(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        boolean inStr = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (!inStr) {
                if (c == '`') {
                    // strip MySQL backticks
                } else if (c == '\'') {
                    inStr = true;
                    out.append(c);
                } else {
                    out.append(c);
                }
            } else {
                if (c == '\\' && i + 1 < sql.length()) {
                    char next = sql.charAt(++i);
                    switch (next) {
                        case '\'' -> out.append("''");
                        case '"' -> out.append('"');
                        case '\\' -> out.append('\\');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case '0' -> out.append('\0');
                        default -> out.append(next);
                    }
                } else if (c == '\'') {
                    inStr = false;
                    out.append(c);
                } else {
                    out.append(c);
                }
            }
        }
        return out.toString();
    }
}
