package com.joprelys.backend.ai.medication;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(
        prefix = "joprelys.ai.medication-safety",
        name = "knowledge-provider",
        havingValue = "rxnorm")
public class RxNormMedicationKnowledgeProvider implements MedicationKnowledgeProvider {

    private static final Set<String> INGREDIENT_TERM_TYPES = Set.of("IN", "PIN", "MIN");

    private final RestClient restClient;
    private final Duration cacheTtl;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public RxNormMedicationKnowledgeProvider(MedicationSafetyProperties properties) {
        this(buildRestClient(properties.rxnorm()),
                Duration.ofMinutes(Math.max(1, properties.rxnorm().cacheMinutes())));
    }

    RxNormMedicationKnowledgeProvider(RestClient restClient, Duration cacheTtl) {
        this.restClient = restClient;
        this.cacheTtl = cacheTtl.isNegative() || cacheTtl.isZero()
                ? Duration.ofMinutes(1)
                : cacheTtl;
    }

    private static RestClient buildRestClient(MedicationSafetyProperties.RxNormProperties config) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(Math.max(500, config.connectTimeoutMillis())));
        requestFactory.setReadTimeout(Duration.ofMillis(Math.max(500, config.readTimeoutMillis())));
        return RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<MedicationKnowledge> resolve(String drugName) {
        if (drugName == null || drugName.isBlank()) {
            return Optional.empty();
        }
        String key = drugName.trim().toLowerCase();
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.value();
        }

        Optional<MedicationKnowledge> resolved = load(drugName.trim());
        cache.put(key, new CacheEntry(resolved, Instant.now().plus(cacheTtl)));
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private Optional<MedicationKnowledge> load(String drugName) {
        try {
            Map<String, Object> searchResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rxcui.json")
                            .queryParam("name", drugName)
                            .queryParam("search", 2)
                            .build())
                    .retrieve()
                    .body(Map.class);
            String rxcui = firstRxcui(searchResponse);
            if (rxcui == null) {
                return Optional.empty();
            }

            Concept concept = loadConcept(rxcui);
            List<MedicationIngredient> ingredients = loadIngredients(rxcui, concept);
            List<MedicationClass> classes = loadClasses(rxcui);
            return Optional.of(new MedicationKnowledge(
                    "RXNORM",
                    rxcui,
                    concept.name(),
                    ingredients,
                    classes));
        } catch (RuntimeException exception) {
            throw new MedicationKnowledgeUnavailableException(
                    "RxNorm/RxClass referential unavailable", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Concept loadConcept(String rxcui) {
        Map<String, Object> response = restClient.get()
                .uri("/rxcui/{rxcui}/properties.json", rxcui)
                .retrieve()
                .body(Map.class);
        Map<String, Object> properties = map(response == null ? null : response.get("properties"));
        String name = text(properties.get("name"));
        String tty = text(properties.get("tty"));
        return new Concept(rxcui, name == null ? rxcui : name, tty == null ? "" : tty);
    }

    @SuppressWarnings("unchecked")
    private List<MedicationIngredient> loadIngredients(String rxcui, Concept concept) {
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rxcui/{rxcui}/related.json")
                        .queryParam("tty", "IN PIN MIN")
                        .build(rxcui))
                .retrieve()
                .body(Map.class);

        Map<String, Object> relatedGroup = map(response == null ? null : response.get("relatedGroup"));
        List<Map<String, Object>> conceptGroups = maps(relatedGroup.get("conceptGroup"));
        Map<String, MedicationIngredient> unique = new LinkedHashMap<>();
        for (Map<String, Object> group : conceptGroups) {
            String tty = text(group.get("tty"));
            if (!INGREDIENT_TERM_TYPES.contains(tty)) {
                continue;
            }
            for (Map<String, Object> item : maps(group.get("conceptProperties"))) {
                String ingredientRxcui = text(item.get("rxcui"));
                String ingredientName = text(item.get("name"));
                if (ingredientRxcui != null && ingredientName != null) {
                    unique.putIfAbsent(
                            ingredientRxcui,
                            new MedicationIngredient(ingredientRxcui, ingredientName, tty));
                }
            }
        }

        if (unique.isEmpty() && INGREDIENT_TERM_TYPES.contains(concept.tty())) {
            unique.put(concept.rxcui(), new MedicationIngredient(
                    concept.rxcui(), concept.name(), concept.tty()));
        }
        return List.copyOf(unique.values());
    }

    @SuppressWarnings("unchecked")
    private List<MedicationClass> loadClasses(String rxcui) {
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rxclass/class/byRxcui.json")
                        .queryParam("rxcui", rxcui)
                        .queryParam("relaSource", "ATC")
                        .build())
                .retrieve()
                .body(Map.class);
        Map<String, Object> list = map(response == null ? null : response.get("rxclassDrugInfoList"));
        List<MedicationClass> result = new ArrayList<>();
        for (Map<String, Object> item : maps(list.get("rxclassDrugInfo"))) {
            Map<String, Object> classItem = map(item.get("rxclassMinConceptItem"));
            String classId = text(classItem.get("classId"));
            String className = text(classItem.get("className"));
            if (classId == null || className == null) {
                continue;
            }
            result.add(new MedicationClass(
                    text(item.get("relaSource")) == null ? "ATC" : text(item.get("relaSource")),
                    classId,
                    className,
                    text(classItem.get("classType")),
                    text(item.get("rela"))));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private String firstRxcui(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Map<String, Object> idGroup = map(response.get("idGroup"));
        Object ids = idGroup.get("rxnormId");
        if (!(ids instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.getFirst();
        return first == null ? null : first.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> raw ? (Map<String, Object>) raw : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private record Concept(String rxcui, String name, String tty) {
    }

    private record CacheEntry(Optional<MedicationKnowledge> value, Instant expiresAt) {
    }
}
