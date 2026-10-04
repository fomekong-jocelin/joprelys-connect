package com.joprelys.backend.prescription.api;

import com.joprelys.backend.prescription.application.PharmacyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/pharmacy/prescriptions")
public class PharmacyController {

	private final PharmacyService pharmacyService;

	public PharmacyController(PharmacyService pharmacyService) {
		this.pharmacyService = pharmacyService;
	}

	@PostMapping("/verify")
	public PharmacyVerifyResponse verify(@Valid @RequestBody PharmacyVerifyRequest request) {
		return pharmacyService.verifyPrescription(request.prescriptionNumber(), request.pinCode());
	}

	@PostMapping("/dispense")
	@ResponseStatus(HttpStatus.NO_CONTENT)
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('PHARMACY_DISPENSE')")
	public void dispense(@Valid @RequestBody PharmacyDispenseRequest request) {
		pharmacyService.dispensePrescription(request);
	}

    @PostMapping("/validate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('PHARMACY_VALIDATE')")
    public void validate(@Valid @RequestBody PharmacyValidationRequest request) {
        pharmacyService.validatePrescription(request);
    }

	@PostMapping("/history")
	public List<PharmacyDispensationHistoryResponse> history(@Valid @RequestBody PharmacyVerifyRequest request) {
		return pharmacyService.getDispensationHistory(request.prescriptionNumber(), request.pinCode());
	}
}
