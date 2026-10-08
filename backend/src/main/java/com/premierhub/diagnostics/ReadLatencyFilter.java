package com.premierhub.diagnostics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

final class ReadLatencyFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ReadLatencyFilter.class);
    static final String PROBE_PATH = "/api/diagnostics/read-latency";
    private static final Set<String> WORKLOADS = Set.of("select1", "clubs", "players", "fantasy", "minigame");
    private final boolean probeEnabled;
    private final String key, proxySecret;
    private final ReadLatencyProbe probe;
    private final JsonMapper json;
    private final Semaphore operator = new Semaphore(1);

    ReadLatencyFilter(Environment environment, ReadLatencyProbe probe, JsonMapper json) {
        this.probe = probe;
        this.json = json;
        probeEnabled = environment.getProperty("premierhub.diagnostics.read-latency.probe-enabled", Boolean.class, false);
        key = environment.getProperty("PREMIERHUB_READ_LATENCY_KEY", "");
        proxySecret = environment.getProperty("PREMIERHUB_AUTH_PROXY_SECRET", "");
        if (probeEnabled && (!key.matches("[a-fA-F0-9]{64}") || !proxySecret.matches("[a-fA-F0-9]{64}")
                || key.equals(proxySecret))) {
            throw new IllegalArgumentException("Read probes require a separate 32-byte hexadecimal key and proxy secret");
        }
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().equals(PROBE_PATH)) { operatorProbe(request, response); return; }
        String route = route(request.getRequestURI());
        if (!request.getMethod().equals("GET") || route == null) { chain.doFilter(request, response); return; }
        try (ReadTrace trace = ReadTrace.open()) {
            boolean completed = false;
            try { chain.doFilter(request, response); completed = true; }
            finally { LOG.info("READ_LATENCY {}", json.writeValueAsString(trace.report(route, completed ? response.getStatus() : 500))); }
        }
    }
    private void operatorProbe(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        if (!probeEnabled || !request.getMethod().equals("GET")
                || !matches(key, request.getHeader("X-PrismaXI-Read-Latency-Key"))
                || !matches(proxySecret, request.getHeader("X-PrismaXI-Proxy-Secret"))) {
            response.setStatus(404); return;
        }
        String workload = request.getParameter("workload");
        if (!WORKLOADS.contains(workload == null ? "" : workload)) { response.setStatus(400); return; }
        if (!operator.tryAcquire()) { response.setStatus(429); return; }
        try (ReadTrace trace = ReadTrace.open()) {
            boolean available;
            try { available = probe.run(workload); }
            catch (RuntimeException failure) {
                response.setStatus(503);
                response.setContentType("application/json");
                response.getWriter().write(json.writeValueAsString(Map.of("available", false,
                        "measurement", trace.report("probe/" + workload, 503))));
                return;
            }
            response.setContentType("application/json");
            response.getWriter().write(json.writeValueAsString(Map.of("available", available,
                    "measurement", trace.report("probe/" + workload, 200))));
        } finally { operator.release(); }
    }
    private static boolean matches(String expected, String supplied) {
        return supplied != null && supplied.length() == 64
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), supplied.getBytes(StandardCharsets.US_ASCII));
    }
    static String route(String path) {
        if (path.matches("/api/(clubs|players)(/[0-9]+)?")) return path.replaceAll("/[0-9]+", "/{id}");
        if (path.matches("/api/fantasy/2026/me/gameweeks/[0-9]+")) return "/api/fantasy/2026/me/gameweeks/{gw}";
        if (path.matches("/api/minigame/2026/player-guess/((practice|daily)/current|games/[A-Za-z0-9-]+|daily/history|players)"))
            return path.replaceAll("/games/[A-Za-z0-9-]+", "/games/{id}");
        return null;
    }
}
