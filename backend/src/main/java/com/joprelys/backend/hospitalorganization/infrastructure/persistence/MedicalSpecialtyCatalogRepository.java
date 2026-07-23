package com.joprelys.backend.hospitalorganization.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalSpecialtyCatalogRepository extends JpaRepository<MedicalSpecialtyCatalogEntity, String> {
    List<MedicalSpecialtyCatalogEntity> findAllByActiveTrueOrderByNameFrAsc();
}
