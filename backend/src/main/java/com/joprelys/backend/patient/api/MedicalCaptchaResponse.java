package com.joprelys.backend.patient.api;

import java.util.UUID;

public record MedicalCaptchaResponse(
        UUID captchaId,
        String question
) {}
