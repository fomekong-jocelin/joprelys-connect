package com.joprelys.backend.medication.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(
        name = {
            "joprelys.medication-reference.enabled",
            "joprelys.medication-reference.rxnorm.enabled"
        },
        havingValue = "true")
final class RxNormMedicationReferencePort implements MedicationReferencePort {

    private static final String SOURCE = "RXNORM";

    private final RestClient restClient;

    RxNormMedicationReferencePort(MedicationReferenceProperties properties) {
        this(RestClient.builder()
                .baseUrl(properties.getRxnorm().getBaseUrl())
                .build());
    }

    RxNormMedicationReferencePort(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String source() {
        return SOURCE;
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public List<MedicationConcept> findCandidates(String query, int maximum) {
        List<String> identifiers = findIdentifiers(query);
        if (identifiers.isEmpty()) {
            return List.of();
        }

        List<MedicationConcept> concepts = new ArrayList<>();
        for (String identifier : identifiers.stream().limit(Math.max(1, maximum)).toList()) {
            MedicationConcept concept = loadConcept(identifier);
            if (concept != null) {
                concepts.add(concept);
            }
        }
        return List.copyOf(concepts);
    }

    @SuppressWarnings("unchecked")
    private List<String> findIdentifiers(String query) {
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/REST/Prescribe/rxcui.json")
                        .queryParam("name", query)
                        .queryParam("search", 2)
                        .build())
                .retrieve()
                .body(Map.class);
        if (response == null || !(response.get("idGroup") instanceof Map<?, ?> idGroup)) {
            return List.of();
        }
        Object values = idGroup.get("rxnormId");
        if (!(values instanceof List<?> identifiers)) {
            return List.of();
        }
        return identifiers.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(value -> !value.isBlank())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private MedicationConcept loadConcept(String identifier) {
        Map<String, Object> response = restClient.get()
                .uri("/REST/rxcui/{rxcui}/properties.json", identifier)
                .retrieve()
                .body(Map.class);
        if (response == null || !(response.get("properties") instanceof Map<?, ?> properties)) {
            return null;
        }

        String name = stringValue(properties.get("name"));
        if (name == null || name.isBlank()) {
            return null;
        }
        String synonym = stringValue(properties.get("synonym"));
        String termType = stringValue(properties.get("tty"));
        String suppress = stringValue(properties.get("suppress"));
        return new MedicationConcept(
                SOURCE,
                identifier,
                name,
                termType,
                synonym == null || synonym.isBlank() ? List.of() : List.of(synonym),
                !"Y".equalsIgnoreCase(suppress));
    }

    private String stringValue(Object value) {
        return value instanceof String text ? text.trim() : null;
    }
}
