package com.joprelys.backend.appointment.api;

import com.joprelys.backend.appointment.application.PatientAppointmentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints du parcours de rendez-vous du patient authentifié. */
@RestController
@RequestMapping("/api/patient/appointments")
@PreAuthorize("hasAuthority('PATIENT_APPOINTMENT_MANAGE')")
public class PatientAppointmentController {

    private final PatientAppointmentService patientAppointmentService;

    public PatientAppointmentController(PatientAppointmentService patientAppointmentService) {
        this.patientAppointmentService = patientAppointmentService;
    }

    @GetMapping("/doctors")
    public List<DoctorDirectoryEntry> listDoctors(
            @RequestParam(required = false) String specialtyCode,
            @RequestParam(required = false) UUID organizationalUnitId,
            Authentication authentication) {
        return patientAppointmentService.listDoctors(authentication, specialtyCode, organizationalUnitId);
    }

    @GetMapping("/slots")
    public List<SlotResponse> listSlots(
            @RequestParam UUID doctorId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            Authentication authentication) {
        return patientAppointmentService.listSlots(authentication, doctorId, from, to);
    }

    @PostMapping
    public AppointmentResponse book(
            @Valid @RequestBody PatientBookAppointmentRequest request,
            Authentication authentication) {
        return patientAppointmentService.book(authentication, request);
    }

    @GetMapping
    public List<AppointmentResponse> listOwn(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Authentication authentication) {
        return patientAppointmentService.listOwn(authentication, from, to);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelAppointmentRequest request,
            Authentication authentication) {
        return patientAppointmentService.cancel(authentication, id, request);
    }
}
