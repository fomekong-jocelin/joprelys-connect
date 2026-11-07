package com.joprelys.backend.prescription.application;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Chiffre au démarrage les PIN encore stockés en clair (colonne historique {@code pin_code}), puis les efface.
 * Idempotent : ne traite que les lignes qui ont un PIN en clair et pas encore de PIN chiffré.
 * Ne journalise jamais de PIN, seulement des compteurs.
 */
@Component
public class PrescriptionPinEncryptionBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionPinEncryptionBackfill.class);
    private static final int BATCH_SIZE = 500;

    private record PlainPin(UUID id, String pin) {}

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final PrescriptionPinCipher cipher;

    public PrescriptionPinEncryptionBackfill(
            JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate, PrescriptionPinCipher cipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.cipher = cipher;
    }

    @Override
    public void run(ApplicationArguments args) {
        int total = 0;
        int processed;
        do {
            processed = encryptBatch();
            total += processed;
        } while (processed == BATCH_SIZE);
        if (total > 0) {
            log.info("PIN d'ordonnance : {} PIN en clair chiffrés puis effacés.", total);
        }
    }

    int encryptBatch() {
        Integer done = transactionTemplate.execute(status -> {
            List<PlainPin> rows = jdbcTemplate.query("""
                    SELECT id, pin_code FROM prescriptions
                    WHERE pin_code IS NOT NULL AND pin_ciphertext IS NULL
                    FETCH FIRST %d ROWS ONLY
                    """.formatted(BATCH_SIZE),
                    (rs, i) -> new PlainPin(rs.getObject("id", UUID.class), rs.getString("pin_code")));
            for (PlainPin row : rows) {
                jdbcTemplate.update(
                        "UPDATE prescriptions SET pin_ciphertext = ?, pin_code = NULL WHERE id = ?",
                        cipher.encrypt(row.pin()), row.id());
            }
            return rows.size();
        });
        return done == null ? 0 : done;
    }
}
