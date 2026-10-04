package com.premierhub.accounts;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

public class AuthProxyFilter extends OncePerRequestFilter {
    static final String VERIFIED_IP = AuthProxyFilter.class.getName() + ".clientIp";
    static final String PROXY_SECRET = "X-PrismaXI-Proxy-Secret";
    private final AuthProxySettings settings;

    public AuthProxyFilter(AuthProxySettings settings) { this.settings = settings; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !(path.equals("/api/auth") || path.startsWith("/api/auth/")
                || path.startsWith("/api/fantasy/2026/admin/"));
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                             FilterChain chain) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("CDN-Cache-Control", "no-store");
        response.setHeader("Vercel-CDN-Cache-Control", "no-store");
        if (!settings.enabled()) { chain.doFilter(request, response); return; }
        String ip = AuthClientIpResolver.canonicalIp(singleHeader(request, "X-Vercel-Forwarded-For"));
        if (!settings.accepts(singleHeader(request, PROXY_SECRET)) || ip == null) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"AUTH_PROXY_REQUIRED\",\"message\":\"Yêu cầu xác thực phải đi qua ứng dụng PrismaXI.\"}");
            return;
        }
        request.setAttribute(VERIFIED_IP, ip);
        var origin = settings.origin();
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public boolean isSecure() { return true; }
            @Override public String getScheme() { return "https"; }
            @Override public String getServerName() { return origin.getHost(); }
            @Override public int getServerPort() { return origin.getPort() == -1 ? 443 : origin.getPort(); }
            @Override public StringBuffer getRequestURL() { return new StringBuffer(origin + getRequestURI()); }
            // Do not expose the shared secret to controllers or later filters.
            @Override public String getHeader(String name) { return hidden(name) ? null : super.getHeader(name); }
            @Override public Enumeration<String> getHeaders(String name) {
                return hidden(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
            }
            @Override public Enumeration<String> getHeaderNames() {
                return Collections.enumeration(Collections.list(super.getHeaderNames()).stream().filter(name -> !hidden(name)).toList());
            }
            private boolean hidden(String name) {
                return PROXY_SECRET.equalsIgnoreCase(name) || "Forwarded".equalsIgnoreCase(name)
                        || name.regionMatches(true, 0, "X-Forwarded-", 0, 12);
            }
        }, response);
    }

    private static String singleHeader(HttpServletRequest request, String name) {
        var values = request.getHeaders(name);
        if (!values.hasMoreElements()) return null;
        String value = values.nextElement();
        return values.hasMoreElements() ? null : value;
    }
}
