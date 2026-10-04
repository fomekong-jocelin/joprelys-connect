package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.hospitalization.api.HospitalizationPlacementOptions;
import com.joprelys.backend.hospitalization.api.HospitalizationPlacementOptions.*;
import com.joprelys.backend.hospitalorganization.application.HospitalOrganizationUseCase;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.spatial.api.BedResponse;
import com.joprelys.backend.spatial.application.HospitalBedConfigurationService;
import com.joprelys.backend.spatial.application.HospitalLocationConfigurationService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Projections métier en lecture seule, sans droit d'administration implicite. */
@Service
public class HospitalizationPlacementQueryService implements HospitalizationPlacementUseCase {
    private final HospitalOrganizationUseCase organization;
    private final HospitalLocationConfigurationService locations;
    private final HospitalBedConfigurationService beds;
    private final UserAccountRepository users;
    private final StaffOrganizationalUnitAssignmentRepository assignments;
    private final PatientService patients;
    private final VisitRepository visits;

    public HospitalizationPlacementQueryService(HospitalOrganizationUseCase organization,
            HospitalLocationConfigurationService locations, HospitalBedConfigurationService beds,
            UserAccountRepository users, StaffOrganizationalUnitAssignmentRepository assignments,
            PatientService patients, VisitRepository visits) {
        this.organization = organization;
        this.locations = locations;
        this.beds = beds;
        this.users = users;
        this.assignments = assignments;
        this.patients = patients;
        this.visits = visits;
    }

    @Transactional(readOnly = true)
    public HospitalizationPlacementOptions options() {
        UUID tenant = requireTenant();
        return new HospitalizationPlacementOptions(
                organization.listUnits(tenant, false), organization.listServiceCatalog(),
                locations.listSpaces(tenant, null, false).stream()
                        .filter(space -> space.inpatientProfile()).toList(),
                locations.listAssignments(tenant, null, null, Instant.now()), practitioners());
    }

    @Transactional(readOnly = true)
    public List<PractitionerOption> practitioners() {
        UUID tenant = requireTenant();
        Instant now = Instant.now();
        return users.findAllByOrganizationId(tenant).stream()
                .filter(user -> user.hasRole("MEDECIN") || user.hasRole("INFIRMIER"))
                .map(user -> new PractitionerOption(user.getId(), user.getDisplayName(), user.hasRole("MEDECIN") ? "MEDECIN" : "INFIRMIER", user.isEnabled(),
                        assignments.findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(tenant, user.getId())
                                .stream().filter(assignment -> assignment.activeAt(now))
                                .map(assignment -> new UnitOption(assignment.getOrganizationalUnitId()))
                                .distinct().toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BedResponse> beds(UUID spaceId) {
        return beds.listBeds(requireTenant(), spaceId);
    }

    @Transactional(readOnly = true)
    public List<AdmissionVisit> visits(UUID patientId) {
        UUID tenant = requireTenant();
        var patient = patients.getPatientById(patientId);
        if (!tenant.equals(patient.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable dans cet établissement.");
        }
        return visits.findByPatientIdWithPatientAndVitals(patientId).stream()
                .map(visit -> new AdmissionVisit(visit.getId(), visit.getVisitNumber(), visit.getReason(),
                        visit.getCreatedAt(), visit.getStatus())).toList();
    }

    private UUID requireTenant() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise.");
        }
        UUID tenant = users.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .filter(UserAccountEntity::isEnabled).map(UserAccountEntity::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Collaborateur non rattaché à un établissement."));
        if (!tenant.equals(TenantContext.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Établissement non accessible.");
        }
        return tenant;
    }
}
