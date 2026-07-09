package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.api.CreateOperatingReportRequest;
import com.joprelys.backend.hospitalization.api.CreateSurgicalImplantRequest;
import com.joprelys.backend.hospitalization.api.OperatingReportResponse;
import com.joprelys.backend.hospitalization.infrastructure.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OperatingReportService {

    private final HospitalizationRepository hospitalizationRepository;
    private final OperatingReportRepository operatingReportRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public OperatingReportService(HospitalizationRepository hospitalizationRepository,
                                  OperatingReportRepository operatingReportRepository,
                                  UserAccountRepository userAccountRepository,
                                  AuditService auditService) {
        this.hospitalizationRepository = hospitalizationRepository;
        this.operatingReportRepository = operatingReportRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public OperatingReportResponse createOperatingReport(UUID hospitalizationId, CreateOperatingReportRequest request) {
        HospitalizationEntity hosp = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        if (!"EN_COURS".equals(hosp.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de créer un CRO pour une hospitalisation clôturée");
        }

        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor != null ? actor.getOrganizationId() : null;

        OperatingReportEntity report = new OperatingReportEntity(
                hospitalizationId,
                request.procedureName(),
                request.operationDate() != null ? request.operationDate() : Instant.now()
        );
        if (orgId != null) {
            report.setOrganizationId(orgId);
        }
        report.setSurgeonId(request.surgeonId());
        report.setAnesthetistId(request.anesthetistId());
        report.setPreOperativeDiagnosis(request.preOperativeDiagnosis());
        report.setPostOperativeDiagnosis(request.postOperativeDiagnosis());
        report.setProcedureDescription(request.procedureDescription());
        report.setAnesthesiaType(request.anesthesiaType());
        report.setAnesthesiaDescription(request.anesthesiaDescription());
        report.setkSurgeonValue(request.kSurgeonValue());
        report.setkAnesthesistValue(request.kAnesthesistValue());
        report.setkBlocValue(request.kBlocValue());

        if (request.implants() != null) {
            for (CreateSurgicalImplantRequest ir : request.implants()) {
                SurgicalImplantEntity implant = new SurgicalImplantEntity(
                        report,
                        hospitalizationId,
                        ir.implantName(),
                        ir.quantity(),
                        ir.unitPrice()
                );
                if (orgId != null) {
                    implant.setOrganizationId(orgId);
                }
                implant.setLotNumber(ir.lotNumber());
                implant.setManufacturer(ir.manufacturer());
                report.getImplants().add(implant);
            }
        }

        OperatingReportEntity saved = operatingReportRepository.save(report);

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    hosp.getPatientId(),
                    "HOSPITALIZATION",
                    hospitalizationId,
                    "CREATE_OPERATING_REPORT",
                    "Création du compte-rendu opératoire : " + request.procedureName()
            );
        }

        return OperatingReportResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<OperatingReportResponse> getOperatingReports(UUID hospitalizationId) {
        hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        return operatingReportRepository.findByHospitalizationIdOrderByOperationDateDesc(hospitalizationId).stream()
                .map(OperatingReportResponse::fromEntity)
                .toList();
    }

    @Transactional
    public OperatingReportResponse validateOperatingReport(UUID reportId) {
        OperatingReportEntity report = operatingReportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte-rendu opératoire introuvable"));

        if (report.isValidated()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce compte-rendu opératoire est déjà validé et ne peut plus être modifié");
        }

        UserAccountEntity actor = getCurrentUser();
        String validatedBy = actor != null ? actor.getDisplayName() : "Médecin Validateur";

        report.setValidated(true);
        report.setValidatedBy(validatedBy);
        report.setValidatedAt(Instant.now());

        OperatingReportEntity saved = operatingReportRepository.save(report);

        HospitalizationEntity hosp = hospitalizationRepository.findById(report.getHospitalizationId()).orElse(null);
        UUID patientId = hosp != null ? hosp.getPatientId() : null;

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    patientId,
                    "HOSPITALIZATION",
                    report.getHospitalizationId(),
                    "VALIDATE_OPERATING_REPORT",
                    "Validation du compte-rendu opératoire (ID: " + reportId + ")"
            );
        }

        return OperatingReportResponse.fromEntity(saved);
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }
}
