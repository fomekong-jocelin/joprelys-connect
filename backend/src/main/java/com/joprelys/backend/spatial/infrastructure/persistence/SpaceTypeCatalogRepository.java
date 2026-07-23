package com.joprelys.backend.spatial.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceTypeCatalogRepository extends JpaRepository<SpaceTypeCatalogEntity, String> {
    List<SpaceTypeCatalogEntity> findAllByActiveTrueOrderByNameFrAsc();
}
