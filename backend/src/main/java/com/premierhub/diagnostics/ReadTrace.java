package com.premierhub.diagnostics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Request-local measurements. SQL, parameters and application data never leave this class. */
final class ReadTrace implements AutoCloseable {
    private static final ThreadLocal<ReadTrace> CURRENT = new ThreadLocal<>();
    private final long started = System.nanoTime();
    private final Map<String, Query> queries = new LinkedHashMap<>();
    private long connectionNanos, executeNanos, fetchNanos, controlNanos;
    private int acquisitions, reusedConnections, statements, sessionStatements, executeCalls;

    record Query(String kind, boolean session, int count, double executeMs) { }
    record Report(String route, int status, double apiMs, double connectionMs, double sqlExecuteMs,
                  double sqlFetchMs, double jdbcControlMs, int acquisitions, int reusedConnections,
                  int statements, int sessionStatements, int executeCalls, Map<String, Query> queries) { }

    static ReadTrace open() {
        if (CURRENT.get() != null) throw new IllegalStateException("A read trace is already active");
        ReadTrace trace = new ReadTrace();
        CURRENT.set(trace);
        return trace;
    }
    static ReadTrace current() { return CURRENT.get(); }
    void connection(long nanos, boolean reused) {
        connectionNanos += nanos;
        acquisitions++;
        if (reused) reusedConnections++;
    }
    void execution(String sql, long nanos, int items) {
        String normalized = sql == null ? "UNKNOWN" : sql.strip().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        String kind = normalized.split(" ", 2)[0];
        if (!kind.matches("SELECT|INSERT|UPDATE|DELETE|WITH|SET|SHOW|EXPLAIN")) kind = "OTHER";
        boolean session = normalized.contains("SPRING_SESSION");
        String digest;
        try {
            digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        Query previous = queries.get(digest);
        queries.put(digest, new Query(kind, session, (previous == null ? 0 : previous.count()) + items,
                (previous == null ? 0 : previous.executeMs()) + ms(nanos)));
        executeNanos += nanos;
        executeCalls++;
        statements += items;
        if (session) sessionStatements += items;
    }
    void fetch(long nanos) { fetchNanos += nanos; }
    void control(long nanos) { controlNanos += nanos; }
    Report report(String route, int status) {
        return new Report(route, status, ms(System.nanoTime() - started), ms(connectionNanos), ms(executeNanos),
                ms(fetchNanos), ms(controlNanos), acquisitions, reusedConnections, statements, sessionStatements,
                executeCalls, Map.copyOf(queries));
    }
    private static double ms(long nanos) { return nanos / 1_000_000.0; }
    @Override public void close() { CURRENT.remove(); }
}
