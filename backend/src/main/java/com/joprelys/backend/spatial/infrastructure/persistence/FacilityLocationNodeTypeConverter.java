package com.joprelys.backend.spatial.infrastructure.persistence;

import com.joprelys.backend.spatial.domain.FacilityLocationNodeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FacilityLocationNodeTypeConverter implements AttributeConverter<FacilityLocationNodeType, String> {

    @Override
    public String convertToDatabaseColumn(FacilityLocationNodeType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public FacilityLocationNodeType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FacilityLocationNodeType.valueOf(dbData);
    }
}
