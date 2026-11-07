package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.application.PatientAccessGuardService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.visit.application.QrCodeGeneratorService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * QR d'accès du patient, généré côté serveur : l'identifiant patient n'est plus envoyé
 * à un service tiers et la date de validité est fixée par le serveur, pas par le navigateur.
 */
@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasAuthority('PATIENT_PORTAL_ACCESS')")
public class PatientAccessQrController {

    static final Duration VALIDITY = Duration.ofMinutes(15);
    static final String VALID_UNTIL_HEADER = "X-Qr-Valid-Until";
    private static final int QR_SIZE_PX = 448;

    private final PatientAccessGuardService patientAccessGuardService;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final Clock clock;

    @Autowired
    public PatientAccessQrController(
            PatientAccessGuardService patientAccessGuardService,
            QrCodeGeneratorService qrCodeGeneratorService) {
        this(patientAccessGuardService, qrCodeGeneratorService, Clock.systemUTC());
    }

    PatientAccessQrController(
            PatientAccessGuardService patientAccessGuardService,
            QrCodeGeneratorService qrCodeGeneratorService,
            Clock clock) {
        this.patientAccessGuardService = patientAccessGuardService;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.clock = clock;
    }

    @GetMapping(value = "/access-qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getAccessQr(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        Instant validUntil = Instant.now(clock).plus(VALIDITY).truncatedTo(ChronoUnit.SECONDS);
        String payload = "{\"type\":\"JOPRELYS_PATIENT_ACCESS\",\"globalPatientNumber\":\""
                + patient.getGlobalPatientNumber() + "\",\"validUntil\":\"" + validUntil + "\"}";

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(VALID_UNTIL_HEADER, validUntil.toString())
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCodeGeneratorService.generateQrCode(payload, QR_SIZE_PX, QR_SIZE_PX));
    }
}
