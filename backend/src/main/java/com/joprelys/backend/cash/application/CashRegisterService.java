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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CashRegisterService {

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
        getOrCreateDefaultRegister(actor.getOrganizationId()); // assure qu'au moins une caisse existe
        return cashRegisterRepository.findAll().stream()
                .map(CashRegisterResponse::fromEntity)
                .toList();
    }

    @Transactional
    public CashSessionResponse openSession(OpenSessionRequest request) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor.getOrganizationId();

        // 1. Vérifier si l'utilisateur a déjà une session ouverte
        Optional<CashRegisterSessionEntity> activeSession = cashRegisterSessionRepository
                .findByOpenedByUserIdAndStatus(actor.getId(), "OPEN");
        if (activeSession.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vous avez déjà une session de caisse ouverte sur la caisse : " +
                    activeSession.get().getCashRegister().getName());
        }

        // 2. Obtenir ou créer la caisse
        UUID registerId = request.cashRegisterId();
        CashRegisterEntity register;
        if (registerId == null) {
            register = getOrCreateDefaultRegister(orgId);
        } else {
            register = cashRegisterRepository.findById(registerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caisse introuvable"));
        }

        // 3. Ouvrir la session
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

        // Règle BR-HFC-008 : Dépenses > 100 000 FCFA -> Double visa obligatoire
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
        return cashMovementRepository.findByCashRegisterSessionId(sessionId).stream()
                .map(CashMovementResponse::fromEntity)
                .toList();
    }

    @Transactional
    public CashSessionResponse closeSession(CloseSessionRequest request) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor.getOrganizationId();

        CashRegisterSessionEntity session = cashRegisterSessionRepository
                .findByOpenedByUserIdAndStatus(actor.getId(), "OPEN")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Aucune session de caisse ouverte à clôturer."));

        // Calculer le solde théorique
        List<CashMovementEntity> movements = cashMovementRepository.findByCashRegisterSessionId(session.getId());
        double totalIn = movements.stream()
                .filter(m -> "IN".equalsIgnoreCase(m.getMovementType()))
                .mapToDouble(CashMovementEntity::getAmount)
                .sum();
        double totalOut = movements.stream()
                .filter(m -> "OUT".equalsIgnoreCase(m.getMovementType()) || "TRANSFER_TO_BANK".equalsIgnoreCase(m.getMovementType()))
                .mapToDouble(CashMovementEntity::getAmount)
                .sum();

        double closingBalance = session.getOpeningBalance() + totalIn - totalOut;
        double discrepancy = request.declaredBalance() - closingBalance;

        // Si écart non nul et non justifié, lever exception
        if (Math.abs(discrepancy) > 0.01 && (request.discrepancyReason() == null || request.discrepancyReason().trim().isEmpty())) {
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
                "Clôture de session de caisse. Solde théorique: " + closingBalance + " FCFA, Déclaré: " + request.declaredBalance() + " FCFA, Écart: " + discrepancy + " FCFA."
        );

        return CashSessionResponse.fromEntity(saved);
    }

    @Transactional
    public String createReceiptForPayment(PaymentEntity payment) {
        UUID orgId = payment.getOrganizationId();

        // 1. Obtenir le prochain numéro de séquence pour le reçu
        Long nextVal = paymentReceiptRepository.getNextReceiptNumberSequenceValue();
        String formattedSequence = String.format("%06d", nextVal);
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String receiptNumber = "REC-" + todayStr + "-" + formattedSequence;

        // 2. Créer le reçu
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
}
