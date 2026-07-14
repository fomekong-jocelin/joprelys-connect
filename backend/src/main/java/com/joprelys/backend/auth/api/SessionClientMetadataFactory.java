package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.session.application.SessionClientMetadata;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class SessionClientMetadataFactory {

    private static final int USER_AGENT_MAX_LENGTH = 160;

    public SessionClientMetadata from(HttpServletRequest request) {
        return new SessionClientMetadata(
                "WEB",
                normalizeUserAgent(request.getHeader("User-Agent")),
                maskAddress(request.getRemoteAddr()));
    }

    private static String normalizeUserAgent(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("[\\p{Cntrl}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() <= USER_AGENT_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, USER_AGENT_MAX_LENGTH);
    }

    private static String maskAddress(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            byte[] address = InetAddress.getByName(value).getAddress();
            return address.length == 4 ? maskIpv4(address) : maskIpv6(address);
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private static String maskIpv4(byte[] address) {
        return Byte.toUnsignedInt(address[0]) + "."
                + Byte.toUnsignedInt(address[1]) + "."
                + Byte.toUnsignedInt(address[2]) + ".0/24";
    }

    private static String maskIpv6(byte[] address) {
        byte[] prefix = new byte[8];
        System.arraycopy(address, 0, prefix, 0, prefix.length);
        String hex = HexFormat.of().formatHex(prefix);
        return hex.substring(0, 4) + ":" + hex.substring(4, 8) + ":"
                + hex.substring(8, 12) + ":" + hex.substring(12, 16) + "::/64";
    }
}
