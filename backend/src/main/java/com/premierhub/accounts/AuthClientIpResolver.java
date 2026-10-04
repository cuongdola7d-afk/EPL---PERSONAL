package com.premierhub.accounts;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Component
public class AuthClientIpResolver {
    private record Network(byte[] address, int prefix) {
        boolean contains(byte[] candidate) {
            if (candidate.length != address.length) return false;
            for (int bit = 0; bit < prefix; bit++) {
                int mask = 1 << (7 - bit % 8);
                if ((address[bit / 8] & mask) != (candidate[bit / 8] & mask)) return false;
            }
            return true;
        }
    }

    private final List<Network> trusted;

    public AuthClientIpResolver(AuthRateLimitSettings settings,
                               @Value("${server.forward-headers-strategy:none}") String forwarding) {
        // A global ForwardedHeaderFilter/RemoteIpValve would replace the socket peer before trust checks.
        if (!"none".equalsIgnoreCase(forwarding)) {
            throw new IllegalArgumentException("Auth IP trust requires server.forward-headers-strategy=none.");
        }
        trusted = settings.getTrustedProxies().stream().map(AuthClientIpResolver::network).toList();
    }

    public String resolve(HttpServletRequest request) {
        Object verified = request.getAttribute(AuthProxyFilter.VERIFIED_IP);
        if (verified instanceof String ip) return ip;
        byte[] peer = literal(request.getRemoteAddr());
        if (peer == null) return "unknown";
        if (!isTrusted(peer)) return text(peer);
        var headers = request.getHeaders("X-Forwarded-For");
        var chain = new StringBuilder();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            if (chain.length() + header.length() + 1 > 2048) return text(peer);
            if (!chain.isEmpty()) chain.append(',');
            chain.append(header);
        }
        if (chain.isEmpty()) return text(peer);
        String forwarded = chain.toString();
        String[] hops = forwarded.split(",", -1);
        if (hops.length > 32) return text(peer);
        byte[][] addresses = new byte[hops.length][];
        for (int i = 0; i < hops.length; i++) {
            addresses[i] = literal(hops[i].strip());
            if (addresses[i] == null) return text(peer);
        }
        // Walk from the nearest proxy towards the client. Ignore attacker-controlled values to its left.
        for (int i = addresses.length - 1; i >= 0 && isTrusted(peer); i--) peer = addresses[i];
        return text(peer);
    }

    private boolean isTrusted(byte[] address) { return trusted.stream().anyMatch(network -> network.contains(address)); }

    private static Network network(String cidr) {
        String[] parts = cidr.strip().split("/", -1);
        byte[] address = literal(parts[0]);
        if (address == null || parts.length > 2) throw new IllegalArgumentException("Trusted proxies must be literal IP addresses or CIDRs.");
        int prefix;
        try { prefix = parts.length == 1 ? address.length * 8 : Integer.parseInt(parts[1]); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("Invalid trusted proxy prefix."); }
        if (prefix < 1 || prefix > address.length * 8) throw new IllegalArgumentException("Trusted proxy prefix must not trust the entire internet.");
        return new Network(address, prefix);
    }

    private static byte[] literal(String value) {
        if (value == null || value.length() > 45) return null;
        if (value.contains(":") && value.matches("[0-9a-fA-F:.]+")) {
            try { return InetAddress.getByName(value).getAddress(); }
            catch (UnknownHostException invalid) { return null; }
        }
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) return null;
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            if (!parts[i].matches("[0-9]{1,3}")) return null;
            int number = Integer.parseInt(parts[i]);
            if (number > 255) return null;
            bytes[i] = (byte) number;
        }
        return bytes;
    }

    private static String text(byte[] address) {
        try { return InetAddress.getByAddress(address).getHostAddress(); }
        catch (UnknownHostException impossible) { throw new IllegalStateException("Invalid IP length", impossible); }
    }

    static String canonicalIp(String value) {
        byte[] address = literal(value);
        return address == null ? null : text(address);
    }
}
