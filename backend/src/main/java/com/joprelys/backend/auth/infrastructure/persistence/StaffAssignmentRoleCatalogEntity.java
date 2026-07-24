package com.joprelys.backend.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_assignment_role_catalog")
public class StaffAssignmentRoleCatalogEntity {

    @Id
    @Column(name = "code", length = 64)
    private String code;

    @Column(name = "name_fr", nullable = false, length = 120)
    private String nameFr;

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected StaffAssignmentRoleCatalogEntity() {
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

    public boolean isActive() {
        return active;
    }
}
