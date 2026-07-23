package com.joprelys.backend.hospitalorganization.api;

import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogEntity;

public record HospitalServiceCatalogResponse(
        String code,
        String nameFr,
        String nameEn) {

    public static HospitalServiceCatalogResponse fromEntity(HospitalServiceCatalogEntity entity) {
        return new HospitalServiceCatalogResponse(
                entity.getCode(),
                entity.getNameFr(),
                entity.getNameEn());
    }
}
