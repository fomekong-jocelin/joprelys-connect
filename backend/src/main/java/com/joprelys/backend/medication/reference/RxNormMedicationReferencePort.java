package com.joprelys.backend.medication.reference;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
    private static final Set<String> INGREDIENT_TERM_TYPES = Set.of("IN", "PIN", "MIN");

    private final RestClient restClient;

    @Autowired
    RxNormMedicationReferencePort(MedicationReferenceProperties properties) {
        this(buildClient(properties));
    }

    RxNormMedicationReferencePort(RestClient restClient) {
        this.restClient = restClient;
    }

    private static RestClient buildClient(MedicationReferenceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(250, properties.getRxnorm().getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Math.max(500, properties.getRxnorm().getReadTimeoutMs()));
        return RestClient.builder()
                .baseUrl(properties.getRxnorm().getBaseUrl())
                .requestFactory(requestFactory)
                .build();
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

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> findIngredientConceptIds(MedicationConcept concept) {
        if (concept == null || concept.conceptId() == null || concept.conceptId().isBlank()) {
            return Set.of();
        }
        if (INGREDIENT_TERM_TYPES.contains(concept.termType())) {
            return Set.of(concept.conceptId());
        }

        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/REST/rxcui/{rxcui}/related.json")
                        .queryParam("tty", "IN PIN MIN")
                        .build(concept.conceptId()))
                .retrieve()
                .body(Map.class);
        if (response == null || !(response.get("relatedGroup") instanceof Map<?, ?> relatedGroup)) {
            return Set.of();
        }
        Object groupsValue = relatedGroup.get("conceptGroup");
        if (!(groupsValue instanceof List<?> groups)) {
            return Set.of();
        }

        Set<String> ingredientIds = new LinkedHashSet<>();
        for (Object groupValue : groups) {
            if (!(groupValue instanceof Map<?, ?> group)) {
                continue;
            }
            String termType = stringValue(group.get("tty"));
            if (!INGREDIENT_TERM_TYPES.contains(termType)) {
                continue;
            }
            Object conceptsValue = group.get("conceptProperties");
            if (!(conceptsValue instanceof List<?> conceptProperties)) {
                continue;
            }
            for (Object propertyValue : conceptProperties) {
                if (propertyValue instanceof Map<?, ?> properties) {
                    String rxcui = stringValue(properties.get("rxcui"));
                    if (rxcui != null && !rxcui.isBlank()) {
                        ingredientIds.add(rxcui);
                    }
                }
            }
        }
        return Set.copyOf(ingredientIds);
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
