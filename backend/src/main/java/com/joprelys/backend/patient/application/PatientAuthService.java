package com.joprelys.backend.patient.application;

import com.joprelys.backend.auth.api.LoginResponse;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PatientAuthService {

    private final PatientRepository patientRepository;
    private final JwtService jwtService;
    private final Map<String, OtpData> otpMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public PatientAuthService(PatientRepository patientRepository, JwtService jwtService) {
        this.patientRepository = patientRepository;
        this.jwtService = jwtService;
    }

    public String generateAndSendOtp(String globalPatientNumber, String phone, LocalDate birthDate) {
        PatientEntity patient = patientRepository.findByGlobalPatientNumber(globalPatientNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable."));

        if (!normalizePhone(patient.getPhone()).equals(normalizePhone(phone)) || !patient.getBirthDate().equals(birthDate)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Les informations fournies ne correspondent pas.");
        }

        String code = String.format("%06d", random.nextInt(1000000));
        otpMap.put(globalPatientNumber, new OtpData(code, Instant.now(), 0));

        // Impression en console pour la simulation
        System.out.println("[OTP PATIENT] Code de connexion pour DPU " + globalPatientNumber + " : " + code);
        return code;
    }

    public LoginResponse verifyOtp(String globalPatientNumber, String otpCode) {
        OtpData otpData = otpMap.get(globalPatientNumber);

        if (otpData == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune demande de connexion active.");
        }

        if (otpData.isExpired()) {
            otpMap.remove(globalPatientNumber);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code de sécurité a expiré.");
        }

        if (otpData.code().equals(otpCode)) {
            otpMap.remove(globalPatientNumber);
            PatientEntity patient = patientRepository.findByGlobalPatientNumber(globalPatientNumber)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable."));

            JwtService.CreatedToken token = jwtService.createPatientToken(patient);

            return new LoginResponse(
                    token.value(),
                    "Bearer",
                    token.expiresAt(),
                    patient.getGlobalPatientNumber() + "@joprelys.local",
                    patient.getFullName(),
                    "PATIENT"
            );
        } else {
            int newAttempts = otpData.attempts() + 1;
            if (newAttempts >= 3) {
                otpMap.remove(globalPatientNumber);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trop de tentatives infructueuses. Veuillez régénérer un code.");
            } else {
                otpMap.put(globalPatientNumber, new OtpData(otpData.code(), otpData.createdAt(), newAttempts));
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
        patientRepository.findByGlobalPatientNumber(globalPatientNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable."));

        String code = String.format("%06d", random.nextInt(1000000));
        String key = "CONSENT_" + globalPatientNumber + "_" + consentId;
        otpMap.put(key, new OtpData(code, Instant.now(), 0));

        // Simulation : affichage console (en prod → SMS/email)
        System.out.println("[OTP CONSENT] Code pour DPU=" + globalPatientNumber
                + " consentId=" + consentId + " : " + code);
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

    private record OtpData(String code, Instant createdAt, int attempts) {
        public boolean isExpired() {
            return createdAt.plusSeconds(300).isBefore(Instant.now()); // 5 minutes
        }
    }
}
