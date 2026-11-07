package com.joprelys.backend.eco;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/** La copie des contrats doit être identique à eco-contracts (scripts/sync-eco-contracts.mjs), jamais retouchée. */
class EcoContractChecksumTest {

    private static final String BASE = "/eco-contracts/v1/";

    @Test
    void vendoredSchemasMatchPublishedChecksums() throws IOException, NoSuchAlgorithmException {
        List<String> lines = read("SHA256SUMS").lines().filter(line -> !line.isBlank()).toList();
        assertThat(lines).hasSizeGreaterThanOrEqualTo(5);
        for (String line : lines) {
            String[] parts = line.split("\\s+", 2);
            byte[] content = bytes(parts[1].trim());
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
            assertThat(actual).as(parts[1]).isEqualTo(parts[0]);
        }
    }

    @Test
    void validExampleShapeIsAccepted() {
        String link = """
                {"resourceType":"PatientLink","id":"0192f0c4-7a1e-7c3a-9b2e-5d1f00000101",
                 "meta":{"source":"joprelys:clinique-demo","versionId":1},
                 "allopharmaProfileId":"0192f0c4-6e55-7b10-8a00-000000000201","dpu":"DPU-TEST-000123",
                 "issuer":"joprelys:clinique-demo","relationship":"self",
                 "consent":"Consent/0192f0c4-7a20-7d4e-a1b2-5d1f00000301","status":"active",
                 "created":"2026-11-07T09:10:00Z"}""";
        assertThat(EcoContractSchemas.validate("patient-link.schema.json", link)).isEmpty();
        assertThat(EcoContractSchemas.validate("patient-link.schema.json", link.replace("\"status\":\"active\",",
                "\"status\":\"active\",\"name\":\"Interdit\",")))
                .isNotEmpty();
    }

    private static String read(String file) throws IOException {
        return new String(bytes(file), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String file) throws IOException {
        try (InputStream in = EcoContractChecksumTest.class.getResourceAsStream(BASE + file)) {
            assertThat(in).as(file).isNotNull();
            return in.readAllBytes();
        }
    }
}
