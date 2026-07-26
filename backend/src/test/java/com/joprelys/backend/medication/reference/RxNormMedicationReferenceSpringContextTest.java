package com.joprelys.backend.medication.reference;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class RxNormMedicationReferenceSpringContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RxNormTestConfiguration.class);

    @Test
    void shouldCreateRxNormPortWhenMedicationReferenceIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "joprelys.medication-reference.enabled=true",
                        "joprelys.medication-reference.rxnorm.enabled=true",
                        "joprelys.medication-reference.rxnorm.base-url=https://rxnav.test")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RxNormMedicationReferencePort.class);
                    assertThat(context).hasSingleBean(MedicationReferenceProperties.class);
                });
    }

    @Test
    void shouldNotCreateRxNormPortWhenMedicationReferenceIsDisabled() {
        contextRunner
                .withPropertyValues(
                        "joprelys.medication-reference.enabled=false",
                        "joprelys.medication-reference.rxnorm.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(RxNormMedicationReferencePort.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MedicationReferenceProperties.class)
    @Import(RxNormMedicationReferencePort.class)
    static class RxNormTestConfiguration {
    }
}
