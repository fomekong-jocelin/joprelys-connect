package com.joprelys.backend.spatial.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.spatial.api.BedStateChangeResponse;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateAxis;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateChangeEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateChangeRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateChangeSource;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateReasonCode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BedStateChangeService {

    private static final String SYSTEM_ACTOR = "Système Joprelys";

    private static final Set<BedStateReasonCode> CAPACITY_CLOSURE_REASONS = Set.of(
            BedStateReasonCode.CAPACITY_TEMPORARY_CLOSURE,
            BedStateReasonCode.CAPACITY_STAFFING_SHORTAGE,
            BedStateReasonCode.CAPACITY_SAFETY,
            BedStateReasonCode.CAPACITY_OTHER);

    private static final Set<BedStateReasonCode> CLEANING_START_REASONS = Set.of(
            BedStateReasonCode.CLEANING_AFTER_DEPARTURE,
            BedStateReasonCode.CLEANING_AFTER_TRANSFER,
            BedStateReasonCode.CLEANING_ROUTINE,
            BedStateReasonCode.CLEANING_ISOLATION,
            BedStateReasonCode.CLEANING_INCIDENT);

    private static final Set<BedStateReasonCode> MAINTENANCE_START_REASONS = Set.of(
            BedStateReasonCode.MAINTENANCE_PREVENTIVE,
            BedStateReasonCode.MAINTENANCE_CORRECTIVE,
            BedStateReasonCode.MAINTENANCE_SAFETY);

    private final BedStateChangeRepository bedStateChangeRepository;
    private final BedRepository bedRepository;
    private final UserAccountRepository userAccountRepository;

    public BedStateChangeService(
            BedStateChangeRepository bedStateChangeRepository,
            BedRepository bedRepository,
            UserAccountRepository userAccountRepository) {
        this.bedStateChangeRepository = bedStateChangeRepository;
        this.bedRepository = bedRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public BedStateReasonCode parseReasonCode(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif du changement de lit est obligatoire.");
        }
        try {
            return BedStateReasonCode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Motif de changement de lit invalide: " + value);
        }
    }

    @Transactional
    public void recordManual(
            BedEntity bed,
            BedStateAxis axis,
            String previousValue,
            String newValue,
            BedStateReasonCode reasonCode,
            String note) {
        record(
                bed,
                axis,
                previousValue,
                newValue,
                reasonCode,
                note,
                BedStateChangeSource.MANUAL);
    }

    @Transactional
    public void recordLegacySupervision(
            BedEntity bed,
            BedStateAxis axis,
            String previousValue,
            String newValue,
            String note) {
        record(
                bed,
                axis,
                previousValue,
                newValue,
                BedStateReasonCode.LEGACY_SUPERVISION,
                note,
                BedStateChangeSource.LEGACY_SUPERVISION);
    }

    @Transactional
    public void recordSystem(
            BedEntity bed,
            BedStateAxis axis,
            String previousValue,
            String newValue,
            BedStateReasonCode reasonCode,
            String note,
            BedStateChangeSource source) {
        if (source != BedStateChangeSource.SYSTEM_TRANSFER
                && source != BedStateChangeSource.SYSTEM_PHYSICAL_DEPARTURE) {
            throw new IllegalArgumentException("La source fournie n'est pas une source système autorisée.");
        }
        record(bed, axis, previousValue, newValue, reasonCode, note, source);
    }

    @Transactional(readOnly = true)
    public List<BedStateChangeResponse> listHistory(UUID bedId) {
        bedRepository.findById(bedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lit introuvable"));
        return bedStateChangeRepository.findByBedIdOrderByOccurredAtDesc(bedId).stream()
                .map(BedStateChangeResponse::fromEntity)
                .toList();
    }

    private void record(
            BedEntity bed,
            BedStateAxis axis,
            String previousValue,
            String newValue,
            BedStateReasonCode reasonCode,
            String note,
            BedStateChangeSource source) {
        String normalizedNote = normalizeNote(note);
        validateReason(axis, previousValue, newValue, reasonCode, normalizedNote, source);

        UserAccountEntity actor = currentUserOrNull();
        bedStateChangeRepository.save(new BedStateChangeEntity(
                bed,
                axis,
                previousValue,
                newValue,
                reasonCode,
                normalizedNote,
                actor == null ? null : actor.getId(),
                actor == null ? SYSTEM_ACTOR : actor.getDisplayName(),
                source,
                Instant.now()));
    }

    private void validateReason(
            BedStateAxis axis,
            String previousValue,
            String newValue,
            BedStateReasonCode reasonCode,
            String note,
            BedStateChangeSource source) {
        if (source == BedStateChangeSource.LEGACY_SUPERVISION) {
            requireReason(reasonCode == BedStateReasonCode.LEGACY_SUPERVISION);
            return;
        }

        boolean allowed = switch (axis) {
            case CAPACITY -> validateCapacityReason(newValue, reasonCode);
            case READINESS -> validateReadinessReason(previousValue, newValue, reasonCode, source);
        };
        requireReason(allowed);

        if ((reasonCode == BedStateReasonCode.CAPACITY_OTHER
                || reasonCode == BedStateReasonCode.CLEANING_INCIDENT)
                && note == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une note explicative est obligatoire pour ce motif.");
        }
    }

    private boolean validateCapacityReason(String newValue, BedStateReasonCode reasonCode) {
        if ("OPEN".equals(newValue)) {
            return reasonCode == BedStateReasonCode.CAPACITY_REOPENING;
        }
        if ("CLOSED".equals(newValue)) {
            return CAPACITY_CLOSURE_REASONS.contains(reasonCode);
        }
        return false;
    }

    private boolean validateReadinessReason(
            String previousValue,
            String newValue,
            BedStateReasonCode reasonCode,
            BedStateChangeSource source) {
        if ("CLEANING".equals(newValue)) {
            if (source == BedStateChangeSource.SYSTEM_TRANSFER) {
                return reasonCode == BedStateReasonCode.CLEANING_AFTER_TRANSFER;
            }
            if (source == BedStateChangeSource.SYSTEM_PHYSICAL_DEPARTURE) {
                return reasonCode == BedStateReasonCode.CLEANING_AFTER_DEPARTURE;
            }
            return CLEANING_START_REASONS.contains(reasonCode);
        }
        if ("MAINTENANCE".equals(newValue)) {
            return source == BedStateChangeSource.MANUAL && MAINTENANCE_START_REASONS.contains(reasonCode);
        }
        if ("READY".equals(newValue) && "CLEANING".equals(previousValue)) {
            return source == BedStateChangeSource.MANUAL
                    && reasonCode == BedStateReasonCode.CLEANING_COMPLETED;
        }
        if ("READY".equals(newValue) && "MAINTENANCE".equals(previousValue)) {
            return source == BedStateChangeSource.MANUAL
                    && reasonCode == BedStateReasonCode.MAINTENANCE_COMPLETED;
        }
        return false;
    }

    private void requireReason(boolean allowed) {
        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le motif fourni n'est pas compatible avec cette transition de lit.");
        }
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String normalized = note.trim();
        if (normalized.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La note du changement de lit ne peut pas dépasser 500 caractères.");
        }
        return normalized;
    }

    private UserAccountEntity currentUserOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElse(null);
    }
}
