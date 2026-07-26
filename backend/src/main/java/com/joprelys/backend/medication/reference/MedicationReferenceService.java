package com.joprelys.backend.medication.reference;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MedicationReferenceService {

    private static final Logger log = LoggerFactory.getLogger(MedicationReferenceService.class);

    private final MedicationReferenceProperties properties;
    private final List<MedicationReferencePort> providers;
    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public MedicationReferenceService(
            MedicationReferenceProperties properties,
            List<MedicationReferencePort> providers) {
        this.properties = properties;
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(MedicationReferencePort::priority))
                .toList();
    }

    public MedicationReferenceLookup lookup(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isBlank()) {
            return MedicationReferenceLookup.unresolved(query, false, "MEDICATION_QUERY_EMPTY");
        }
        if (!properties.isEnabled()) {
            return MedicationReferenceLookup.unresolved(query, false, "MEDICATION_REFERENCE_DISABLED");
        }

        String key = normalize(query);
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.lookup();
        }

        MedicationReferenceLookup resolved = resolve(query);
        cache.put(key, new CacheEntry(resolved, expiry()));
        return resolved;
    }

    public MedicationEquivalence compare(String leftName, String rightName) {
        MedicationReferenceLookup left = lookup(leftName);
        MedicationReferenceLookup right = lookup(rightName);
        if (!left.resolved() || !right.resolved()) {
            return MedicationEquivalence.unknown("MEDICATION_CONCEPT_NOT_RESOLVED");
        }

        Set<String> rightIds = right.candidates().stream()
                .filter(candidate -> candidate.source().equals(left.source()))
                .map(MedicationConcept::conceptId)
                .collect(Collectors.toSet());
        return left.candidates().stream()
                .filter(candidate -> rightIds.contains(candidate.conceptId()))
                .findFirst()
                .map(candidate -> new MedicationEquivalence(
                        MedicationEquivalence.Status.SAME_CONCEPT,
                        candidate.source(),
                        candidate.conceptId(),
                        candidate.conceptId(),
                        "MEDICATION_SHARED_REFERENCE_CONCEPT"))
                .orElseGet(() -> MedicationEquivalence.unknown("NO_SHARED_REFERENCE_CONCEPT"));
    }

    void clearCache() {
        cache.clear();
    }

    private MedicationReferenceLookup resolve(String query) {
        boolean providerFailed = false;
        int maximum = Math.max(1, Math.min(20, properties.getMaxCandidates()));
        for (MedicationReferencePort provider : providers) {
            try {
                List<MedicationConcept> candidates = provider.findCandidates(query, maximum);
                if (candidates != null && !candidates.isEmpty()) {
                    return new MedicationReferenceLookup(
                            query,
                            true,
                            false,
                            provider.source(),
                            candidates.stream().limit(maximum).toList(),
                            null,
                            Instant.now());
                }
            } catch (RuntimeException exception) {
                providerFailed = true;
                log.warn("Medication reference provider unavailable source={}", provider.source());
            }
        }
        return MedicationReferenceLookup.unresolved(
                query,
                providerFailed,
                providerFailed ? "MEDICATION_REFERENCE_UNAVAILABLE" : "MEDICATION_NOT_RESOLVED");
    }

    private Instant expiry() {
        int minutes = Math.max(1, properties.getCacheTtlMinutes());
        return Instant.now().plus(Duration.ofMinutes(minutes));
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private record CacheEntry(
            MedicationReferenceLookup lookup,
            Instant expiresAt) {
    }
}
