package com.joprelys.backend.patient.application;

import com.joprelys.backend.auth.api.LoginResponse;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.notification.application.AccountMailService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PatientAuthService {

    private final PatientRepository patientRepository;
    private final JwtService jwtService;
    private final Map<String, OtpData> otpMap = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final AccountMailService accountMailService;

    public PatientAuthService(PatientRepository patientRepository, JwtService jwtService, AccountMailService accountMailService) {
        this.patientRepository = patientRepository;
        this.jwtService = jwtService;
        this.accountMailService = accountMailService;
    }

    public String generateAndSendOtp(String globalPatientNumber, String phone, LocalDate birthDate) {
        String normalizedDpu = normalizePatientNumber(globalPatientNumber);
        PatientEntity patient = patientRepository.findByGlobalPatientNumber(normalizedDpu)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable."));

        if (!normalizePhone(patient.getPhone()).equals(normalizePhone(phone)) || !patient.getBirthDate().equals(birthDate)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Les informations fournies ne correspondent pas.");
        }

        String code = String.format("%06d", random.nextInt(1000000));
        if (patient.getEmail() == null || patient.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune adresse e-mail n'est associée à ce patient.");
        }
        accountMailService.sendPatientLoginCode(patient.getEmail(), patient.getFullName(), code);
        otpMap.put(normalizedDpu, new OtpData(code, Instant.now(), 0));
        return code;
    }

    public LoginResponse verifyOtp(String globalPatientNumber, String otpCode) {
        String normalizedDpu = normalizePatientNumber(globalPatientNumber);
        OtpData otpData = otpMap.get(normalizedDpu);

        if (otpData == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune demande de connexion active.");
        }

        if (otpData.isExpired()) {
            otpMap.remove(normalizedDpu);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code de sécurité a expiré.");
        }

        if (otpData.code().equals(otpCode)) {
            otpMap.remove(normalizedDpu);
            PatientEntity patient = patientRepository.findByGlobalPatientNumber(normalizedDpu)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable."));

            JwtService.CreatedToken token = jwtService.createPatientToken(patient);

            return new LoginResponse(
                    token.value(),
                    "Bearer",
                    token.expiresAt(),
                    patient.getGlobalPatientNumber() + "@joprelys.local",
                    patient.getFullName(),
                    "PATIENT",
                    false
            );
        } else {
            int newAttempts = otpData.attempts() + 1;
            if (newAttempts >= 3) {
                otpMap.remove(normalizedDpu);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trop de tentatives infructueuses. Veuillez régénérer un code.");
            } else {
                otpMap.put(normalizedDpu, new OtpData(otpData.code(), otpData.createdAt(), newAttempts));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code de sécurité incorrect.");
            }
        }
    }

    /**
     * STORY-1909 : Génère un OTP pour valider l'approbation d'un consentement.
     * Utilisé quand le canal de validation est OTP_SMS ou OTP_EMAIL.
     *
     * @param globalPatientNumber le numéro DPU du patient
     * @param consentId           l'identifiant du consentement à approuver
     */
    public void generateConsentOtp(String globalPatientNumber, String consentId) {
        String normalizedDpu = normalizePatientNumber(globalPatientNumber);
        patientRepository.findByGlobalPatientNumber(normalizedDpu)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable."));

        String code = String.format("%06d", random.nextInt(1000000));
        String key = "CONSENT_" + normalizedDpu + "_" + consentId;
        otpMap.put(key, new OtpData(code, Instant.now(), 0));

    }

    /**
     * STORY-1909 : Vérifie l'OTP de consentement et retourne true si valide.
     *
     * @param globalPatientNumber le numéro DPU
     * @param consentId           l'identifiant du consentement
     * @param otpCode             le code saisi par le patient
     */
    public boolean verifyConsentOtp(String globalPatientNumber, String consentId, String otpCode) {
        String key = "CONSENT_" + globalPatientNumber + "_" + consentId;
        OtpData otpData = otpMap.get(key);

        if (otpData == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun OTP actif pour ce consentement.");
        }
        if (otpData.isExpired()) {
            otpMap.remove(key);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code OTP a expiré.");
        }
        if (otpData.code().equals(otpCode)) {
            otpMap.remove(key);
            return true;
        } else {
            int newAttempts = otpData.attempts() + 1;
            if (newAttempts >= 3) {
                otpMap.remove(key);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Trop de tentatives. Veuillez régénérer un OTP.");
            }
            otpMap.put(key, new OtpData(otpData.code(), otpData.createdAt(), newAttempts));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code OTP incorrect.");
        }
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() >= 9) {
            return digits.substring(digits.length() - 9);
        }
        return digits;
    }

    /**
     * Normalise le numéro DPU : supprime les espaces de début/fin et convertit en majuscules.
     * Rend la recherche robuste aux erreurs de saisie courantes (casse, espaces).
     */
    private static String normalizePatientNumber(String number) {
        if (number == null) return "";
        return number.trim().toUpperCase();
    }

    private record OtpData(String code, Instant createdAt, int attempts) {
        public boolean isExpired() {
            return createdAt.plusSeconds(300).isBefore(Instant.now()); // 5 minutes
        }
    }
}
