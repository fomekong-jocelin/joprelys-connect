package com.joprelys.backend.patient.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MedicalCaptchaService {

    private static final Map<UUID, CaptchaEntry> CAPTCHA_REGISTRY = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static final List<CaptchaDefinition> QUESTIONS = List.of(
            new CaptchaDefinition("Quelle est la température corporelle normale moyenne de l'homme en degrés Celsius ? (Répondre par le nombre uniquement)", "37"),
            new CaptchaDefinition("Quel organe est principalement responsable du pompage du sang ?", "coeur"),
            new CaptchaDefinition("Combien de poumons possède un être humain en bonne santé ?", "2"),
            new CaptchaDefinition("Quel est le liquide rouge qui circule dans nos veines ?", "sang"),
            new CaptchaDefinition("Combien de doigts possède une main humaine standard ?", "5"),
            new CaptchaDefinition("Quel organe principal nous permet de respirer ?", "poumons"),
            new CaptchaDefinition("Quelle structure dure et blanche compose notre squelette ?", "os"),
            new CaptchaDefinition("Quel organe est le siège de la pensée et du système nerveux central ?", "cerveau")
    );

    public GeneratedCaptcha generateCaptcha() {
        UUID id = UUID.randomUUID();
        CaptchaDefinition definition = QUESTIONS.get(random.nextInt(QUESTIONS.size()));
        CAPTCHA_REGISTRY.put(id, new CaptchaEntry(definition.answer, Instant.now().plusSeconds(600))); // Expire dans 10 minutes
        return new GeneratedCaptcha(id, definition.question);
    }

    public boolean validateCaptcha(UUID id, String answer) {
        if (id == null || answer == null) {
            return false;
        }
        CaptchaEntry entry = CAPTCHA_REGISTRY.remove(id); // Suppression pour éviter le rejeu
        if (entry == null) {
            return false;
        }
        if (entry.expiryTime.isBefore(Instant.now())) {
            return false; // Expiré
        }
        return normalizeString(answer).equals(normalizeString(entry.answer));
    }

    @Scheduled(cron = "0 */5 * * * ?") // Toutes les 5 minutes
    public void cleanupExpiredCaptchas() {
        Instant now = Instant.now();
        CAPTCHA_REGISTRY.entrySet().removeIf(entry -> entry.getValue().expiryTime.isBefore(now));
    }

    private String normalizeString(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        return normalized.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    private static class CaptchaDefinition {
        String question;
        String answer;

        CaptchaDefinition(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }

    private static class CaptchaEntry {
        String answer;
        Instant expiryTime;

        CaptchaEntry(String answer, Instant expiryTime) {
            this.answer = answer;
            this.expiryTime = expiryTime;
        }
    }

    public record GeneratedCaptcha(UUID id, String question) {}
}
