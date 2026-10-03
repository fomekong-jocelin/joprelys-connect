package com.joprelys.backend.hospitalorganization.application;

import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Libellés affichables des unités d'un établissement : nom propre, ou libellé du catalogue pour un service.
 * Une visite saisie au niveau d'un service doit concerner aussi les soignants de ses unités de soins.
 */
@Service
public class OrganizationalUnitNamesService {

	private final OrganizationalUnitRepository unitRepository;
	private final HospitalServiceCatalogRepository catalogRepository;

	public OrganizationalUnitNamesService(
			OrganizationalUnitRepository unitRepository,
			HospitalServiceCatalogRepository catalogRepository) {
		this.unitRepository = unitRepository;
		this.catalogRepository = catalogRepository;
	}

	/** Structure active d'un établissement, avec les libellés résolus. */
	@Transactional(readOnly = true)
	public UnitDirectory directory(UUID organizationId) {
		List<OrganizationalUnitEntity> units = unitRepository.findAllByOrganizationIdAndActiveTrueOrderByCodeAsc(organizationId);
		Map<String, String> catalogNames = catalogRepository.findAllByActiveTrueOrderByNameFrAsc().stream()
				.collect(Collectors.toMap(HospitalServiceCatalogEntity::getCode, HospitalServiceCatalogEntity::getNameFr, (a, b) -> a));
		return new UnitDirectory(units, catalogNames);
	}

	public static final class UnitDirectory {
		private final List<OrganizationalUnitEntity> units;
		private final Map<UUID, OrganizationalUnitEntity> byId;
		private final Map<String, String> catalogNames;

		private UnitDirectory(List<OrganizationalUnitEntity> units, Map<String, String> catalogNames) {
			this.units = units;
			this.byId = units.stream().collect(Collectors.toMap(OrganizationalUnitEntity::getId, unit -> unit));
			this.catalogNames = catalogNames;
		}

		public List<OrganizationalUnitEntity> units() {
			return units;
		}

		public OrganizationalUnitEntity get(UUID unitId) {
			return byId.get(unitId);
		}

		public String name(OrganizationalUnitEntity unit) {
			if (unit.getName() != null && !unit.getName().isBlank()) return unit.getName();
			String catalogName = unit.getServiceCatalogCode() != null ? catalogNames.get(unit.getServiceCatalogCode()) : null;
			return catalogName != null ? catalogName : unit.getCode();
		}

		/** Nom de chaque unité et de ses unités parentes (unité de soins → service → département…). */
		public Set<String> namesWithAncestors(Collection<UUID> unitIds) {
			Set<String> names = new LinkedHashSet<>();
			for (UUID unitId : unitIds) {
				OrganizationalUnitEntity current = byId.get(unitId);
				int guard = 0;
				while (current != null && guard++ < 10) {
					names.add(name(current));
					current = current.getParentId() != null ? byId.get(current.getParentId()) : null;
				}
			}
			return names;
		}
	}
}
