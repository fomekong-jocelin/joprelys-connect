package com.joprelys.backend.patient.application;

import com.joprelys.backend.auth.api.LoginResponse;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.OtpChallengeStore;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.notification.application.AccountMailService;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientAuthService {

    /**
     * Message unique pour tout refus de demande de code (patient inconnu, informations non
     * concordantes, patient décédé, absence d'e-mail) : aucun motif ne doit permettre
     * d'énumérer les dossiers patients (SEC-06).
     */
    static final String GENERIC_OTP_REQUEST_FAILURE =
            "Les informations fournies ne permettent pas d'envoyer un code de connexion. "
                    + "Vérifiez-les ou contactez votre établissement de santé.";
    static final String OTP_REQUEST_LOCKED =
            "Trop de tentatives infructueuses. Veuillez réessayer dans quelques minutes.";

    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    /** Les essais échoués survivent à une redemande de code pendant cette fenêtre. */
    private static final Duration LOGIN_ATTEMPT_WINDOW = Duration.ofMinutes(15);

    private final PatientRepository patientRepository;
    private final JwtService jwtService;
    private final OtpChallengeStore loginChallenges;
    private final OtpChallengeStore consentChallenges;
    private final SecureRandom random = new SecureRandom();
    private final AccountMailService accountMailService;

    public PatientAuthService(
            PatientRepository patientRepository,
            JwtService jwtService,
            AccountMailService accountMailService,
            Clock clock) {
        this.patientRepository = patientRepository;
        this.jwtService = jwtService;
        this.accountMailService = accountMailService;
        this.loginChallenges = new OtpChallengeStore(clock, OTP_TTL, MAX_OTP_ATTEMPTS, LOGIN_ATTEMPT_WINDOW);
        this.consentChallenges = new OtpChallengeStore(clock, OTP_TTL, MAX_OTP_ATTEMPTS, Duration.ZERO);
    }

    public String generateAndSendOtp(String globalPatientNumber, String phone, LocalDate birthDate) {
        String normalizedDpu = normalizePatientNumber(globalPatientNumber);
        PatientEntity patient = patientRepository.findByGlobalPatientNumber(normalizedDpu)
                .filter(candidate -> identityMatches(candidate, phone, birthDate))
                .filter(candidate -> candidate.getEmail() != null && !candidate.getEmail().isBlank())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, GENERIC_OTP_REQUEST_FAILURE));

        String code = String.format("%06d", random.nextInt(1000000));
        if (!loginChallenges.issue(normalizedDpu, code)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, OTP_REQUEST_LOCKED);
        }
        try {
            accountMailService.sendPatientLoginCode(patient.getEmail(), patient.getFullName(), code);
        } catch (RuntimeException deliveryFailure) {
            // Code jamais reçu : il est retiré, les essais échoués déjà comptés sont conservés.
            loginChallenges.discardCode(normalizedDpu);
            throw deliveryFailure;
        }
        return code;
    }

    public LoginResponse verifyOtp(String globalPatientNumber, String otpCode) {
        String normalizedDpu = normalizePatientNumber(globalPatientNumber);
        OtpChallengeStore.Verification verification = loginChallenges.verify(normalizedDpu, otpCode);
        return switch (verification.outcome()) {
            case VERIFIED -> loginResponse(normalizedDpu);
            case NO_ACTIVE_CHALLENGE -> throw badRequest("Aucune demande de connexion active.");
            case EXPIRED -> throw badRequest("Le code de sécurité a expiré.");
            case TOO_MANY_ATTEMPTS -> throw badRequest("Trop de tentatives infructueuses. Veuillez régénérer un code.");
            case INCORRECT -> throw badRequest("Code de sécurité incorrect.");
        };
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
        consentChallenges.issue(consentKey(normalizedDpu, consentId), code);
    }

    /**
     * STORY-1909 : Vérifie l'OTP de consentement et retourne true si valide.
     *
     * @param globalPatientNumber le numéro DPU
     * @param consentId           l'identifiant du consentement
     * @param otpCode             le code saisi par le patient
     */
    public boolean verifyConsentOtp(String globalPatientNumber, String consentId, String otpCode) {
        String key = consentKey(normalizePatientNumber(globalPatientNumber), consentId);
        OtpChallengeStore.Verification verification = consentChallenges.verify(key, otpCode);
        return switch (verification.outcome()) {
            case VERIFIED -> true;
            case NO_ACTIVE_CHALLENGE -> throw badRequest("Aucun OTP actif pour ce consentement.");
            case EXPIRED -> throw badRequest("Le code OTP a expiré.");
            case TOO_MANY_ATTEMPTS -> throw badRequest("Trop de tentatives. Veuillez régénérer un OTP.");
            case INCORRECT -> throw badRequest("Code OTP incorrect.");
        };
    }

    Optional<String> activeLoginCodeForTesting(String globalPatientNumber) {
        return loginChallenges.activeCodeForTesting(normalizePatientNumber(globalPatientNumber));
    }

    private LoginResponse loginResponse(String normalizedDpu) {
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
    }

    private static boolean identityMatches(PatientEntity patient, String phone, LocalDate birthDate) {
        return !patient.isDeceased()
                && normalizePhone(patient.getPhone()).equals(normalizePhone(phone))
                && patient.getBirthDate() != null
                && patient.getBirthDate().equals(birthDate);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static String consentKey(String normalizedDpu, String consentId) {
        return normalizedDpu + "_" + consentId;
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
        return number.trim().toUpperCase(Locale.ROOT);
    }
}
