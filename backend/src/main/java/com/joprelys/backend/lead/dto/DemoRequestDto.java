package com.joprelys.backend.lead.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DemoRequestDto(
    @NotBlank(message = "Le nom et prénom sont obligatoires")
    @Size(max = 255)
    String fullName,

    @NotBlank(message = "Le nom de l'établissement est obligatoire")
    @Size(max = 255)
    String organizationName,

    @Size(max = 100)
    String role,

    @NotBlank(message = "Le numéro de téléphone ou WhatsApp est obligatoire")
    @Size(max = 50)
    String phone,

    @Size(max = 255)
    String email,

    @Size(max = 100)
    String city,

    @Size(max = 2000)
    String message,

    @Size(max = 50)
    String source,

    @Size(max = 10)
    String locale
) {}
