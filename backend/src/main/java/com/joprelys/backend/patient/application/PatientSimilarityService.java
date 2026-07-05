package com.joprelys.backend.patient.application;

import com.joprelys.backend.patient.infrastructure.persistence.PatientDuplicateCandidateEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientDuplicateCandidateRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Service
public class PatientSimilarityService {

    private static final Logger log = LoggerFactory.getLogger(PatientSimilarityService.class);

    private final PatientRepository patientRepository;
    private final PatientDuplicateCandidateRepository duplicateCandidateRepository;

    public PatientSimilarityService(PatientRepository patientRepository,
                                    PatientDuplicateCandidateRepository duplicateCandidateRepository) {
        this.patientRepository = patientRepository;
        this.duplicateCandidateRepository = duplicateCandidateRepository;
    }

    public String normalize(String str) {
        if (str == null) return "";
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        return normalized.toLowerCase().replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
    }

    public int calculateLevenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[len1][len2];
    }

    public double getSimilarityScore(String s1, String s2) {
        String n1 = normalize(s1);
        String n2 = normalize(s2);
        if (n1.isEmpty() && n2.isEmpty()) return 100.0;
        if (n1.isEmpty() || n2.isEmpty()) return 0.0;
        int distance = calculateLevenshteinDistance(n1, n2);
        int maxLen = Math.max(n1.length(), n2.length());
        return (1.0 - (double) distance / maxLen) * 100.0;
    }

    @Transactional
    public List<PatientDuplicateCandidateEntity> checkForDuplicates(PatientEntity newPatient) {
        log.info("Checking duplicates for patient: {} (ID: {})", newPatient.getFullName(), newPatient.getId());
        List<PatientEntity> sameBirthdatePatients = patientRepository.findByBirthDate(newPatient.getBirthDate());
        List<PatientDuplicateCandidateEntity> candidates = new ArrayList<>();

        for (PatientEntity existing : sameBirthdatePatients) {
            if (existing.getId().equals(newPatient.getId())) {
                continue;
            }
            if (!"ACTIVE".equals(existing.getStatus())) {
                continue;
            }

            double score = getSimilarityScore(newPatient.getFullName(), existing.getFullName());
            if (score >= 85.0) {
                log.info("Potential duplicate found: {} and {} with score {}%", newPatient.getFullName(), existing.getFullName(), score);
                
                PatientEntity source = newPatient;
                PatientEntity target = existing;
                if (newPatient.getId().compareTo(existing.getId()) > 0) {
                    source = existing;
                    target = newPatient;
                }

                var existingCandidate = duplicateCandidateRepository.findByPatientPair(source.getId(), target.getId());
                if (existingCandidate.isEmpty()) {
                    PatientDuplicateCandidateEntity candidate = new PatientDuplicateCandidateEntity(source, target, score);
                    candidates.add(duplicateCandidateRepository.save(candidate));
                }
            }
        }
        return candidates;
    }
}