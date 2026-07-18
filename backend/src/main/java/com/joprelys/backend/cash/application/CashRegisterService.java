package com.joprelys.backend.cash.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentEntity;
import com.joprelys.backend.cash.api.*;
import com.joprelys.backend.cash.infrastructure.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class CashRegisterService {

    private static final DateTimeFormatter RECEIPT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter CLOSEOUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault());

    private final CashRegisterRepository cashRegisterRepository;
    private final CashRegisterSessionRepository cashRegisterSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public CashRegisterService(CashRegisterRepository cashRegisterRepository,
                               CashRegisterSessionRepository cashRegisterSessionRepository,
                               CashMovementRepository cashMovementRepository,
                               PaymentReceiptRepository paymentReceiptRepository,
                               UserAccountRepository userAccountRepository,
                               AuditService auditService) {
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashRegisterSessionRepository = cashRegisterSessionRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    private UserAccountEntity getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non connecté"));
    }

    @Transactional
    public CashRegisterEntity getOrCreateDefaultRegister(UUID orgId) {
        return cashRegisterRepository.findByCode("CAISSE-PRINCIPALE")
                .orElseGet(() -> {
                    CashRegisterEntity defaultRegister = new CashRegisterEntity("CAISSE-PRINCIPALE", "Caisse Principale");
                    defaultRegister.setOrganizationId(orgId);
                    return cashRegisterRepository.save(defaultRegister);
                });
    }

    @Transactional(readOnly = true)
    public List<CashRegisterResponse> listRegisters() {
        UserAccountEntity actor = getCurrentUser();
        getOrCreateDefaultRegister(actor.getOrganizationId());
        return cashRegisterRepository.findAll().stream()
                .map(CashRegisterResponse::fromEntity)
                .toList();
    }

    @Transactional
    public CashSessionResponse openSession(OpenSessionRequest request) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor.getOrganizationId();

        Optional<CashRegisterSessionEntity> activeSession = cashRegisterSessionRepository
                .findByOpenedByUserIdAndStatus(actor.getId(), "OPEN");
        if (activeSession.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vous avez déjà une session de caisse ouverte sur la caisse : " +
                            activeSession.get().getCashRegister().getName());
        }

        UUID registerId = request.cashRegisterId();
        CashRegisterEntity register;
        if (registerId == null) {
            register = getOrCreateDefaultRegister(orgId);
        } else {
            register = cashRegisterRepository.findById(registerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caisse introuvable"));
        }

        CashRegisterSessionEntity session = new CashRegisterSessionEntity(register, actor.getId(), request.openingBalance());
        session.setOrganizationId(orgId);
        CashRegisterSessionEntity saved = cashRegisterSessionRepository.save(session);

        auditService.logSuccess(
                actor.getId(),
                orgId,
                null,
                "CASH",
                saved.getId(),
                "OPEN_CASH_SESSION",
                "Ouverture de session de caisse pour " + register.getName() + " avec un fond de " + request.openingBalance() + " FCFA."
        );

        return CashSessionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public CashSessionResponse getActiveSession() {
        UserAccountEntity actor = getCurrentUser();
        return cashRegisterSessionRepository
                .findByOpenedByUserIdAndStatus(actor.getId(), "OPEN")
                .map(CashSessionResponse::fromEntity)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<CashRegisterSessionEntity> findActiveSessionForUser(UUID userId) {
        return cashRegisterSessionRepository.findByOpenedByUserIdAndStatus(userId, "OPEN");
    }

    @Transactional
    public CashMovementResponse addMovement(CashMovementRequest request) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor.getOrganizationId();

        CashRegisterSessionEntity session = cashRegisterSessionRepository
                .findByOpenedByUserIdAndStatus(actor.getId(), "OPEN")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Impossible d'enregistrer un mouvement : aucune session de caisse n'est ouverte."));

        validateMovement(request);

        if ("OUT".equalsIgnoreCase(request.movementType()) && request.amount() > 100000.0) {
            if (request.doubleVisaApproved() == null || !request.doubleVisaApproved()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Double visa DAF + Médecin Chef requis pour les dépenses supérieures à 100 000 FCFA.");
            }
        }

        CashMovementEntity movement = new CashMovementEntity(
                session,
                request.movementType().toUpperCase(),
                request.amount(),
                request.description(),
                request.paymentMethod().toUpperCase(),
                request.referenceNumber(),
                actor.getId()
        );
        movement.setOrganizationId(orgId);
        CashMovementEntity saved = cashMovementRepository.save(movement);

        auditService.logSuccess(
                actor.getId(),
                orgId,
                null,
                "CASH",
                saved.getId(),
                "ADD_CASH_MOVEMENT",
                "Mouvement " + request.movementType() + " de " + request.amount() + " FCFA enregistré sur la session " + session.getId() + "."
        );

        return CashMovementResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CashMovementResponse> getSessionMovements(UUID sessionId) {
        UserAccountEntity actor = getCurrentUser();
        CashRegisterSessionEntity session = findSession(sessionId);
        assertSessionAccess(actor, session);
        return cashMovementRepository.findByCashRegisterSessionId(sessionId).stream()
                .map(CashMovementResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CashSessionSummaryResponse getActiveSessionSummary() {
        UserAccountEntity actor = getCurrentUser();
        CashRegisterSessionEntity session = cashRegisterSessionRepository
                .findByOpenedByUserIdAndStatus(actor.getId(), "OPEN")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune session de caisse ouverte."));
        return reconcile(session);
    }

    @Transactional
    public CashSessionResponse closeSession(CloseSessionRequest request) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor.getOrganizationId();

        CashRegisterSessionEntity session = cashRegisterSessionRepository
                .findByOpenedByUserIdAndStatus(actor.getId(), "OPEN")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Aucune session de caisse ouverte à clôturer."));

        CashSessionSummaryResponse reconciliation = reconcile(session);
        double closingBalance = reconciliation.expectedCash();
        double discrepancy = request.declaredBalance() - closingBalance;

        if (Math.abs(discrepancy) > 0.01
                && (request.discrepancyReason() == null || request.discrepancyReason().trim().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un écart de caisse de " + discrepancy + " FCFA a été détecté. Une justification est obligatoire pour clôturer.");
        }

        session.setStatus("CLOSED");
        session.setClosedByUserId(actor.getId());
        session.setClosedAt(java.time.Instant.now());
        session.setClosingBalance(closingBalance);
        session.setDeclaredBalance(request.declaredBalance());
        session.setDiscrepancyAmount(discrepancy);
        session.setDiscrepancyReason(request.discrepancyReason());

        CashRegisterSessionEntity saved = cashRegisterSessionRepository.save(session);

        auditService.logSuccess(
                actor.getId(),
                orgId,
                null,
                "CASH",
                saved.getId(),
                "CLOSE_CASH_SESSION",
                "Clôture de session de caisse. Solde théorique: " + closingBalance + " FCFA, Déclaré: "
                        + request.declaredBalance() + " FCFA, Écart: " + discrepancy + " FCFA."
        );

        return CashSessionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CashSessionHistoryResponse> listMySessions() {
        UserAccountEntity actor = getCurrentUser();
        return cashRegisterSessionRepository.findTop20ByOpenedByUserIdOrderByOpenedAtDesc(actor.getId()).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CashSessionHistoryResponse getSessionHistory(UUID sessionId) {
        UserAccountEntity actor = getCurrentUser();
        CashRegisterSessionEntity session = findSession(sessionId);
        assertSessionAccess(actor, session);
        return toHistoryResponse(session);
    }

    @Transactional(readOnly = true)
    public CashRegisterSessionEntity getClosedSessionForReport(UUID sessionId) {
        UserAccountEntity actor = getCurrentUser();
        CashRegisterSessionEntity session = findSession(sessionId);
        assertSessionAccess(actor, session);
        if (!"CLOSED".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le bordereau de clôture est disponible uniquement pour une session clôturée.");
        }
        return session;
    }

    public String buildCloseoutReportNumber(CashRegisterSessionEntity session) {
        java.time.Instant referenceDate = session.getClosedAt() != null ? session.getClosedAt() : session.getOpenedAt();
        String suffix = session.getId().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "CLS-" + CLOSEOUT_DATE_FORMAT.format(referenceDate) + "-" + suffix;
    }

    private CashSessionHistoryResponse toHistoryResponse(CashRegisterSessionEntity session) {
        CashSessionSummaryResponse summary = reconcile(session);
        String openedByName = resolveUserName(session.getOpenedByUserId());
        String closedByName = resolveUserName(session.getClosedByUserId());
        return CashSessionHistoryResponse.from(
                session,
                summary,
                openedByName,
                closedByName,
                buildCloseoutReportNumber(session)
        );
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userAccountRepository.findById(userId)
                .map(UserAccountEntity::getDisplayName)
                .orElse("Utilisateur inconnu");
    }

    private CashRegisterSessionEntity findSession(UUID sessionId) {
        return cashRegisterSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session de caisse introuvable."));
    }

    private void assertSessionAccess(UserAccountEntity actor, CashRegisterSessionEntity session) {
        boolean supervisor = hasCurrentAuthority("CASH_DISCREPANCY_RESOLVE");
        boolean owner = session.getOpenedByUserId().equals(actor.getId());
        if (!supervisor && !owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez consulter que vos propres sessions de caisse.");
        }
    }

    private boolean hasCurrentAuthority(String expectedAuthority) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> expectedAuthority.equals(authority.getAuthority()));
    }

    private CashSessionSummaryResponse reconcile(CashRegisterSessionEntity session) {
        List<CashMovementEntity> movements = cashMovementRepository.findByCashRegisterSessionId(session.getId());
        return CashSessionReconciliationCalculator.calculate(session, movements);
    }

    private void validateMovement(CashMovementRequest request) {
        String type = request.movementType().toUpperCase();
        String method = request.paymentMethod().toUpperCase();
        if (!List.of("IN", "OUT", "TRANSFER_TO_BANK").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de mouvement non pris en charge.");
        }
        if (!List.of("CASH", "CHECK", "BANK_TRANSFER").contains(method)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mode de règlement non pris en charge.");
        }
        if ("TRANSFER_TO_BANK".equals(type)
                && (!"CASH".equals(method) || request.referenceNumber() == null || request.referenceNumber().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un versement banque doit correspondre à des espèces et contenir une référence de dépôt.");
        }
    }

    @Transactional
    public String createReceiptForPayment(PaymentEntity payment) {
        UUID orgId = payment.getOrganizationId();
        Long nextVal = paymentReceiptRepository.getNextReceiptNumberSequenceValue();
        String formattedSequence = String.format("%06d", nextVal);
        String todayStr = LocalDate.now().format(RECEIPT_DATE_FORMAT);
        String receiptNumber = "REC-" + todayStr + "-" + formattedSequence;

        PaymentReceiptEntity receipt = new PaymentReceiptEntity(payment, receiptNumber);
        receipt.setOrganizationId(orgId);
        PaymentReceiptEntity saved = paymentReceiptRepository.save(receipt);
        return saved.getReceiptNumber();
    }

    @Transactional(readOnly = true)
    public PaymentReceiptResponse getReceiptByPayment(UUID paymentId) {
        PaymentReceiptEntity entity = paymentReceiptRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun reçu généré pour ce règlement."));
        return PaymentReceiptResponse.fromEntity(entity);
    }

    @Transactional(readOnly = true)
    public List<CashSessionResponse> listSessionsByRegister(UUID registerId) {
        return cashRegisterSessionRepository.findByCashRegisterIdOrderByOpenedAtDesc(registerId).stream()
                .map(CashSessionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CashSessionResponse> listAllSessions() {
        return cashRegisterSessionRepository.findAllByOrderByOpenedAtDesc().stream()
                .map(CashSessionResponse::fromEntity)
                .toList();
    }

    @Transactional
    public CashSessionResponse resolveDiscrepancy(UUID sessionId, ResolveDiscrepancyRequest request) {
        UserAccountEntity currentUser = getCurrentUser();
        CashRegisterSessionEntity session = cashRegisterSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session introuvable."));

        if (!"CLOSED".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seules les sessions clôturées peuvent faire l'objet d'une résolution d'écart.");
        }

        session.setDiscrepancyResolved(true);
        session.setResolutionNotes(request.resolutionNotes());
        session.setResolvedAt(java.time.Instant.now());
        session.setResolvedByUserId(currentUser.getId().toString());

        CashRegisterSessionEntity saved = cashRegisterSessionRepository.save(session);
        auditService.logSuccess(
                currentUser.getId(),
                currentUser.getOrganizationId(),
                null,
                "CASH_SESSION",
                saved.getId(),
                "RESOLVE_DISCREPANCY",
                "Resolution de l'ecart de caisse par la DAF"
        );

        return CashSessionResponse.fromEntity(saved);
    }
}
