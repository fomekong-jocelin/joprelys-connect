package com.joprelys.backend.patient.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MedicalSummaryResponse(
        UUID patientId,
        String fullName,
        String globalPatientNumber,
        LocalDate birthDate,
        String bloodGroup,
        String gender,
        List<AllergySummaryDto> allergies,
        List<MedicalHistorySummaryDto> medicalHistory,
        List<TreatmentSummaryDto> activePrescriptions,
        List<VisitSummaryDto> recentVisits,
        List<DiagnosticSummaryDto> recentDiagnostics,
        List<CriticalResultSummaryDto> criticalResults
) {
    public record AllergySummaryDto(
            UUID id,
            String substance,
            String severity,
            String reaction,
            String status,
            LocalDate discoveredAt
    ) {}

    public record MedicalHistorySummaryDto(
            UUID id,
            String category,
            String description,
            LocalDate onsetDate,
            boolean isOngoing,
            boolean important,
            String comment
    ) {}

    public record TreatmentSummaryDto(
            UUID id,
            String prescriptionNumber,
            String status,
            java.time.Instant createdAt,
            List<PrescriptionItemDto> items
    ) {
        public record PrescriptionItemDto(
                String drugName,
                String dosage,
                String posology,
                String duration,
                String quantity,
                String instructions
        ) {}
    }

    public record VisitSummaryDto(
            UUID id,
            String visitNumber,
            String reason,
            String orientation,
            String service,
            java.time.Instant createdAt
    ) {}

    public record DiagnosticSummaryDto(
            UUID consultationId,
            String visitNumber,
            java.time.Instant createdAt,
            String doctorName,
            String suspectedDiagnosis,
            String diagnosis,
            String finalDiagnosis,
            String conclusion
    ) {}

    public record CriticalResultSummaryDto(
            UUID id,
            String resultNumber,
            String analyteName,
            String value,
            String unit,
            String referenceRange,
            String interpretation,
            String validatorName,
            java.time.Instant validatedAt
    ) {}
}
