package com.joprelys.backend.patient.application;

import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.common.config.WebApplicationProperties;
import com.joprelys.backend.visit.application.QrCodeGeneratorService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds the admission QR code server side so the organization identifier is never sent to a third party.
 */
@Service
public class DefaultGenerateAdmissionQrCodeUseCase implements GenerateAdmissionQrCodeUseCase {

    private static final int QR_CODE_SIZE = 300;

    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final WebApplicationProperties webApplicationProperties;

    public DefaultGenerateAdmissionQrCodeUseCase(
            QrCodeGeneratorService qrCodeGeneratorService,
            WebApplicationProperties webApplicationProperties) {
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.webApplicationProperties = webApplicationProperties;
    }

    @Override
    public byte[] generate() {
        UUID organizationId = TenantContext.getTenantId();
        if (organizationId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organisation de l'utilisateur introuvable.");
        }
        return qrCodeGeneratorService.generateQrCode(
                buildRegistrationUrl(organizationId).toASCIIString(),
                QR_CODE_SIZE,
                QR_CODE_SIZE);
    }

    private URI buildRegistrationUrl(UUID organizationId) {
        return UriComponentsBuilder.fromUri(webApplicationProperties.baseUrl())
                .replacePath(null)
                .replaceQuery(null)
                .fragment(null)
                .pathSegment("public", "register")
                .queryParam("orgId", organizationId)
                .build()
                .toUri();
    }
}
