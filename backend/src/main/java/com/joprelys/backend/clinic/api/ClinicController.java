package com.joprelys.backend.clinic.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clinic")
public class ClinicController {

	@GetMapping("/admin")
	@PreAuthorize("hasAuthority('ORGANIZATION_MANAGE')")
	public String getAdminData() {
		return "Données administratives confidentielles";
	}

	@GetMapping("/medecin")
	@PreAuthorize("hasAuthority('CLINICAL_READ')")
	public String getMedecinData() {
		return "Dossiers cliniques des patients";
	}

	@GetMapping("/pharmacien")
	@PreAuthorize("hasAuthority('PHARMACY_PRESCRIPTION_READ')")
	public String getPharmacienData() {
		return "Ordonnances et délivrances en attente";
	}
}
