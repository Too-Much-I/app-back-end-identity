package web.tosunsaeng.identity.domain.support;

import java.net.InetAddress;
import java.util.List;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/** Only literal addresses are accepted; never resolve hostnames from untrusted headers. */
public final class SupportClientAddress {
    private final List<IpAddressMatcher> proxies;
    public SupportClientAddress(List<String> cidrs) {
        proxies = cidrs.stream().map(value -> {
            literal(value.split("/", -1)[0]);
            return new IpAddressMatcher(value);
        }).toList();
    }
    public String resolve(String remote, String forwarded) {
        String current = literal(remote);
        if (forwarded == null || forwarded.isBlank() || !trusted(current)) return current;
        if (forwarded.length() > 2048) throw SupportError.INVALID_REQUEST.exception();
        String[] hops = forwarded.split(",", -1);
        if (hops.length > 20) throw SupportError.INVALID_REQUEST.exception();
        for (int i = hops.length - 1; i >= 0 && trusted(current); i--) current = literal(hops[i].strip());
        return current;
    }
    private boolean trusted(String ip) { return proxies.stream().anyMatch(matcher -> matcher.matches(ip)); }
    static String literal(String value) {
        if (value == null || value.isBlank() || !value.matches("[0-9a-fA-F:.]+")) throw SupportError.INVALID_REQUEST.exception();
        if (!value.contains(":")) {
            String[] parts = value.split("\\.", -1);
            if (parts.length != 4) throw SupportError.INVALID_REQUEST.exception();
            for (String part : parts) {
                if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255) throw SupportError.INVALID_REQUEST.exception();
            }
        }
        try { return InetAddress.getByName(value).getHostAddress(); }
        catch (java.net.UnknownHostException e) { throw SupportError.INVALID_REQUEST.exception(); }
    }
}
