package com.joprelys.backend.hospitalorganization.api;

import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogEntity;
import com.joprelys.backend.spatial.domain.HospitalServiceType;

public record HospitalServiceCatalogResponse(
        String code,
        String nameFr,
        String nameEn,
        HospitalServiceType serviceType) {

    public static HospitalServiceCatalogResponse fromEntity(HospitalServiceCatalogEntity entity) {
        return new HospitalServiceCatalogResponse(
                entity.getCode(),
                entity.getNameFr(),
                entity.getNameEn(),
                entity.getServiceType());
    }
}
