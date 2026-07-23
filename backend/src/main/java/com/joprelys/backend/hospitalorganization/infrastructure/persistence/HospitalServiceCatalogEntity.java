package com.joprelys.backend.hospitalorganization.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hospital_service_catalog")
public class HospitalServiceCatalogEntity {

    @Id
    @Column(length = 64)
    private String code;

    @Column(name = "name_fr", nullable = false, length = 120)
    private String nameFr;

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(name = "service_type", nullable = false, length = 40)
    private String serviceType;

    @Column(nullable = false)
    private boolean active;

    protected HospitalServiceCatalogEntity() {
    }

    public String getCode() {
        return code;
    }

    public String getNameFr() {
        return nameFr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getServiceType() {
        return serviceType;
    }

    public boolean isActive() {
        return active;
    }
}
