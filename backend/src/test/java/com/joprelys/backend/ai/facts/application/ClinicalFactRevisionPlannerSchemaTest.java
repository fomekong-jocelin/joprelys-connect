package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClinicalFactRevisionPlannerSchemaTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldRequireEveryOperationFieldAndUseNestedAnyOfForNullableObjects() {
        Map<String, Object> root = ClinicalFactRevisionPlannerSchema.schema();

        assertThat(root.get("type")).isEqualTo("object");
        assertThat(root.get("additionalProperties")).isEqualTo(false);
        assertThat(root.get("required")).isEqualTo(List.of("operations"));

        Map<String, Object> properties = (Map<String, Object>) root.get("properties");
        Map<String, Object> operations = (Map<String, Object>) properties.get("operations");
        Map<String, Object> operation = (Map<String, Object>) operations.get("items");
        assertThat(operation.get("additionalProperties")).isEqualTo(false);
        assertThat(operation.get("required"))
                .isEqualTo(List.of("type", "targetFactId", "fact", "retraction"));

        Map<String, Object> operationProperties = (Map<String, Object>) operation.get("properties");
        Map<String, Object> fact = (Map<String, Object>) operationProperties.get("fact");
        Map<String, Object> retraction = (Map<String, Object>) operationProperties.get("retraction");
        assertThat(fact).containsKey("anyOf");
        assertThat(retraction).containsKey("anyOf");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldKeepNestedClinicalObjectsClosedAndFullyRequired() {
        Map<String, Object> root = ClinicalFactRevisionPlannerSchema.schema();
        Map<String, Object> properties = (Map<String, Object>) root.get("properties");
        Map<String, Object> operations = (Map<String, Object>) properties.get("operations");
        Map<String, Object> operation = (Map<String, Object>) operations.get("items");
        Map<String, Object> operationProperties = (Map<String, Object>) operation.get("properties");
        Map<String, Object> factNullable = (Map<String, Object>) operationProperties.get("fact");
        List<Map<String, Object>> factAnyOf = (List<Map<String, Object>>) factNullable.get("anyOf");
        Map<String, Object> fact = factAnyOf.getFirst();

        assertThat(fact.get("type")).isEqualTo("object");
        assertThat(fact.get("additionalProperties")).isEqualTo(false);
        assertThat((List<String>) fact.get("required")).containsExactlyInAnyOrder(
                "factType", "authority", "conceptCode", "conceptText", "polarity",
                "valuePrimary", "valueSecondary", "unitCode", "temporalityText",
                "laterality", "frequencyText", "routeText", "evidence");
    }
}
