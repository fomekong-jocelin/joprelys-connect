package com.joprelys.backend.hospitalorganization.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import org.junit.jupiter.api.Test;

class OrganizationalUnitTypeConverterTest {

    private final OrganizationalUnitTypeConverter converter = new OrganizationalUnitTypeConverter();

    @Test
    void shouldPersistEveryUnitTypeUsingItsStableCode() {
        for (OrganizationalUnitType type : OrganizationalUnitType.values()) {
            assertThat(converter.convertToDatabaseColumn(type)).isEqualTo(type.name());
        }
    }

    @Test
    void shouldRestoreEveryUnitTypeFromItsStableCode() {
        for (OrganizationalUnitType type : OrganizationalUnitType.values()) {
            assertThat(converter.convertToEntityAttribute(type.name())).isEqualTo(type);
        }
    }

    @Test
    void shouldRejectUnknownDatabaseCodes() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldKeepNullValuesNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
