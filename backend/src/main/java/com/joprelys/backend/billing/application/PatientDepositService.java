package com.joprelys.backend.billing.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.api.CreateDepositRequest;
import com.joprelys.backend.billing.api.DepositResponse;
import com.joprelys.backend.billing.infrastructure.persistence.PatientDepositEntity;
import com.joprelys.backend.billing.infrastructure.persistence.PatientDepositRepository;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentMethod;
import com.joprelys.backend.cash.api.CashMovementRequest;
import com.joprelys.backend.cash.application.CashRegisterService;
import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Acomptes patient (ADR-20261017 D-16) : encaissés en caisse avant facturation, imputés ensuite sur facture. */
@Service
public class PatientDepositService {

    private final PatientDepositRepository depositRepository;
    private final CashRegisterService cashRegisterService;
    private final PatientCanonicalResolver canonicalResolver;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public PatientDepositService(PatientDepositRepository depositRepository, CashRegisterService cashRegisterService,
                                 PatientCanonicalResolver canonicalResolver, UserAccountRepository userAccountRepository,
                                 AuditService auditService) {
        this.depositRepository = depositRepository;
        this.cashRegisterService = cashRegisterService;
        this.canonicalResolver = canonicalResolver;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DepositResponse collect(CreateDepositRequest request) {
        if (request.method() == PaymentMethod.DEPOSIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mode d'encaissement de l'acompte invalide.");
        }
        UserAccountEntity actor = currentUser();
        var session = cashRegisterService.findActiveSessionForUser(actor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Acompte sans session ouverte : veuillez d'abord ouvrir une session de caisse."));
        UUID patientId = canonicalResolver.resolve(request.patientId()).canonicalPatient().getId();
        String number = String.format("ACP-%s-%06d",
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), depositRepository.nextDepositNumber());
        PatientDepositEntity deposit = new PatientDepositEntity(number, patientId, request.visitId(), request.amount(),
                request.method(), blankToNull(request.reference()), blankToNull(request.note()), session.getId(), actor.getId());
        deposit.setOrganizationId(actor.getOrganizationId());
        PatientDepositEntity saved = depositRepository.save(deposit);
        cashRegisterService.addMovement(new CashMovementRequest("IN", request.amount().doubleValue(),
                "Acompte N° " + number, request.method().name(), request.reference(), false));
        auditService.logSuccess(actor.getId(), actor.getOrganizationId(), patientId, "BILLING", saved.getId(),
                "COLLECT_DEPOSIT", "Acompte " + number + " de " + request.amount() + " FCFA (" + request.method() + ").");
        return DepositResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<DepositResponse> listByPatient(UUID patientId) {
        var context = canonicalResolver.resolve(patientId);
        return depositRepository.findByPatientIdInOrderByCreatedAtDesc(context.contributingPatientIds()).stream()
                .map(DepositResponse::fromEntity)
                .toList();
    }

    private UserAccountEntity currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non connecté"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
