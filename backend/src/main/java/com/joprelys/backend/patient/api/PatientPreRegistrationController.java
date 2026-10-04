package com.joprelys.backend.patient.api;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.common.api.PageResponse;
import com.joprelys.backend.patient.application.GenerateAdmissionQrCodeUseCase;
import com.joprelys.backend.patient.application.MedicalCaptchaService;
import com.joprelys.backend.patient.application.PatientPreRegistrationService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientPreRegistrationEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PreRegistrationStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
public class PatientPreRegistrationController {

    private final PatientPreRegistrationService preRegistrationService;
    private final MedicalCaptchaService captchaService;
    private final UserAccountRepository userAccountRepository;
    private final GenerateAdmissionQrCodeUseCase generateAdmissionQrCode;

    public PatientPreRegistrationController(
            PatientPreRegistrationService preRegistrationService,
            MedicalCaptchaService captchaService,
            UserAccountRepository userAccountRepository,
            GenerateAdmissionQrCodeUseCase generateAdmissionQrCode) {
        this.preRegistrationService = preRegistrationService;
        this.captchaService = captchaService;
        this.userAccountRepository = userAccountRepository;
        this.generateAdmissionQrCode = generateAdmissionQrCode;
    }

    // --- Endpoints Publics ---

    @GetMapping("/api/public/pre-registrations/captcha")
    public MedicalCaptchaResponse getCaptcha() {
        MedicalCaptchaService.GeneratedCaptcha captcha = captchaService.generateCaptcha();
        return new MedicalCaptchaResponse(captcha.id(), captcha.question());
    }

    @PostMapping("/api/public/pre-registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientPreRegistrationResponse submitPreRegistration(@Valid @RequestBody PatientPreRegistrationRequest request) {
        com.joprelys.backend.auth.security.TenantContext.setTenantId(request.organizationId());
        try {
            PatientPreRegistrationEntity entity = preRegistrationService.submitPreRegistration(request);
            return mapToResponse(entity);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.clear();
        }
    }

    // --- Endpoints Privés/Professionnels ---

    @GetMapping("/api/pre-registrations")
    @PreAuthorize("hasAuthority('PATIENT_READ')")
    public PageResponse<PatientPreRegistrationResponse> getPreRegistrations(
            @RequestParam(name = "status", required = false) PreRegistrationStatus status,
            Pageable pageable) {
        return PageResponse.fromPage(preRegistrationService.getPreRegistrations(status, pageable));
    }

    @GetMapping(value = "/api/pre-registrations/admission-qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAuthority('PATIENT_WRITE')")
    public ResponseEntity<byte[]> getAdmissionQrCode() {
        byte[] qrCodeImage = generateAdmissionQrCode.generate();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(qrCodeImage.length)
                .body(qrCodeImage);
    }

    @GetMapping("/api/pre-registrations/{id}")
    @PreAuthorize("hasAuthority('PATIENT_READ')")
    public PatientPreRegistrationResponse getPreRegistrationById(@PathVariable UUID id) {
        return preRegistrationService.getPreRegistrationById(id);
    }

    @PostMapping("/api/pre-registrations/{id}/validate")
    @PreAuthorize("hasAuthority('PATIENT_WRITE')")
    public ValidationResponse validatePreRegistration(
            @PathVariable UUID id,
            @Valid @RequestBody PreRegistrationValidationRequest request) {
        UUID actorId = getCurrentUserActorId();
        UUID patientId = preRegistrationService.validatePreRegistration(id, request, actorId);
        return new ValidationResponse(patientId, "VALIDATED");
    }

    @PostMapping("/api/pre-registrations/{id}/reject")
    @PreAuthorize("hasAuthority('PATIENT_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectPreRegistration(@PathVariable UUID id) {
        UUID actorId = getCurrentUserActorId();
        preRegistrationService.rejectPreRegistration(id, actorId);
    }

    private UUID getCurrentUserActorId() {
        String actorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(actorEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé"))
                .getId();
    }

    private PatientPreRegistrationResponse mapToResponse(PatientPreRegistrationEntity entity) {
        return new PatientPreRegistrationResponse(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getGender(),
                entity.getBirthDate(),
                entity.getBloodGroup(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAddress(),
                entity.getEmergencyContactName(),
                entity.getEmergencyContactPhone(),
                entity.getEmergencyContactRelation(),
                entity.getStatus(),
                entity.getCreatedAt(),
                null,
                null,
                null,
                entity.getValidatedPatientId()
        );
    }

    public record ValidationResponse(UUID patientId, String status) {}
}
