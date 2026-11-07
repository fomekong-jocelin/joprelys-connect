package com.joprelys.backend.eco.infrastructure.webhook;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Signature {@code Eco-Signature} (feuille de route § 2.2) : {@code t=<secondes unix>,v1=<hex HMAC-SHA256>},
 * calculée sur {@code t + "." + corps}. Le récepteur refuse un horodatage décalé de plus de 5 minutes.
 */
public final class EcoWebhookSigner {

    public static final String HEADER = "Eco-Signature";
    private static final String ALGORITHM = "HmacSHA256";

    private EcoWebhookSigner() {
    }

    public static String sign(String secret, long timestampSeconds, String body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal((timestampSeconds + "." + body).getBytes(StandardCharsets.UTF_8));
            return "t=" + timestampSeconds + ",v1=" + HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponible", e);
        }
    }
}
