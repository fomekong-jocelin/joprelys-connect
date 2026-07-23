package com.joprelys.backend.spatial.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inpatient_space_type_catalog")
public class InpatientSpaceTypeCatalogEntity {

    @Id
    @Column(name = "space_type_code", length = 64)
    private String spaceTypeCode;

    protected InpatientSpaceTypeCatalogEntity() {
    }

    public String getSpaceTypeCode() { return spaceTypeCode; }
}
