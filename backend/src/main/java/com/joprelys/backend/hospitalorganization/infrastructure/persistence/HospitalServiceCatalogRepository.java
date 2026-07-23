package com.joprelys.backend.hospitalorganization.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalServiceCatalogRepository extends JpaRepository<HospitalServiceCatalogEntity, String> {
    List<HospitalServiceCatalogEntity> findAllByActiveTrueOrderByNameFrAsc();
}
