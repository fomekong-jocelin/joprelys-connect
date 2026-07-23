package com.joprelys.backend.spatial.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InpatientSpaceTypeCatalogRepository
        extends JpaRepository<InpatientSpaceTypeCatalogEntity, String> {
}
