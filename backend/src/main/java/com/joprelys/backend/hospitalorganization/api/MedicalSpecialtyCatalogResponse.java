package com.joprelys.backend.hospitalorganization.api;

import com.joprelys.backend.hospitalorganization.infrastructure.persistence.MedicalSpecialtyCatalogEntity;

public record MedicalSpecialtyCatalogResponse(
        String code,
        String nameFr,
        String nameEn) {

    public static MedicalSpecialtyCatalogResponse fromEntity(MedicalSpecialtyCatalogEntity entity) {
        return new MedicalSpecialtyCatalogResponse(
                entity.getCode(),
                entity.getNameFr(),
                entity.getNameEn());
    }
}
