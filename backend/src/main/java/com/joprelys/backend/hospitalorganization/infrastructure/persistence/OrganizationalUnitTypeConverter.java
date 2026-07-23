package com.joprelys.backend.hospitalorganization.infrastructure.persistence;

import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OrganizationalUnitTypeConverter implements AttributeConverter<OrganizationalUnitType, String> {

    @Override
    public String convertToDatabaseColumn(OrganizationalUnitType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public OrganizationalUnitType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OrganizationalUnitType.valueOf(dbData);
    }
}
