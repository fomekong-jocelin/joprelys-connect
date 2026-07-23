package com.joprelys.backend.spatial.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "space_type_catalog")
public class SpaceTypeCatalogEntity {

    @Id
    @Column(length = 64)
    private String code;

    @Column(name = "name_fr", nullable = false, length = 120)
    private String nameFr;

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(nullable = false)
    private boolean active;

    protected SpaceTypeCatalogEntity() {
    }

    public String getCode() { return code; }
    public String getNameFr() { return nameFr; }
    public String getNameEn() { return nameEn; }
    public boolean isActive() { return active; }
}
