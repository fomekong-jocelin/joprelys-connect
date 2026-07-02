package com.joprelys.backend.clinic.api;

import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/organizations")
@PreAuthorize("hasRole('ADMIN_JOPRELYS')")
public class OrganizationController {

	private final OrganizationRepository organizationRepository;

	public OrganizationController(OrganizationRepository organizationRepository) {
		this.organizationRepository = organizationRepository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
		if (organizationRepository.existsByEmail(request.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "L'organisation avec cet email existe déjà.");
		}

		var entity = new OrganizationEntity(
				request.name(),
				request.email(),
				request.phone(),
				request.address(),
				request.city()
		);
		var saved = organizationRepository.save(entity);
		return mapToResponse(saved);
	}

	@GetMapping
	public List<OrganizationResponse> list() {
		return organizationRepository.findAll().stream()
				.map(this::mapToResponse)
				.toList();
	}

	@PutMapping("/{id}/status")
	public OrganizationResponse updateStatus(@PathVariable UUID id, @RequestBody String status) {
		String cleanStatus = status.replace("\"", "").trim().toUpperCase();
		if (!cleanStatus.equals("ACTIVE") && !cleanStatus.equals("INACTIVE")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide.");
		}

		var entity = organizationRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation non trouvée."));
		
		entity.setStatus(cleanStatus);
		var saved = organizationRepository.save(entity);
		return mapToResponse(saved);
	}

	private OrganizationResponse mapToResponse(OrganizationEntity entity) {
		return new OrganizationResponse(
				entity.getId(),
				entity.getName(),
				entity.getEmail(),
				entity.getPhone(),
				entity.getAddress(),
				entity.getCity(),
				entity.getLogoPath(),
				entity.getStatus(),
				entity.getCreatedAt()
		);
	}
}
