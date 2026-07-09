package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateOperatingReportRequest(
    UUID surgeonId,
    UUID anesthetistId,
    Instant operationDate,
    String preOperativeDiagnosis,
    String postOperativeDiagnosis,
    @NotBlank(message = "Le nom de la procédure est obligatoire")
    String procedureName,
    String procedureDescription,
    String anesthesiaType,
    String anesthesiaDescription,
    double kSurgeonValue,
    double kAnesthesistValue,
    double kBlocValue,
    List<CreateSurgicalImplantRequest> implants
) {}
