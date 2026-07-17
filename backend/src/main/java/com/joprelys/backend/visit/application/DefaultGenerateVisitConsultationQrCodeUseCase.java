package com.joprelys.backend.visit.application;

import com.joprelys.backend.common.config.WebApplicationProperties;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Default application service for visit consultation QR codes.
 */
@Service
public class DefaultGenerateVisitConsultationQrCodeUseCase
        implements GenerateVisitConsultationQrCodeUseCase {

    private static final String ACTIVE_VISIT_STATUS = "EN_COURS";
    private static final int QR_CODE_SIZE = 300;

    private final VisitService visitService;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final WebApplicationProperties webApplicationProperties;

    public DefaultGenerateVisitConsultationQrCodeUseCase(
            VisitService visitService,
            QrCodeGeneratorService qrCodeGeneratorService,
            WebApplicationProperties webApplicationProperties) {
        this.visitService = visitService;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.webApplicationProperties = webApplicationProperties;
    }

    @Override
    public byte[] generate(UUID visitId) {
        var visit = visitService.getVisit(visitId);
        if (!ACTIVE_VISIT_STATUS.equals(visit.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le QR code est disponible uniquement pour une visite active.");
        }
        URI consultationUrl = buildConsultationUrl(visitId);
        return qrCodeGeneratorService.generateQrCode(
                consultationUrl.toASCIIString(),
                QR_CODE_SIZE,
                QR_CODE_SIZE);
    }

    private URI buildConsultationUrl(UUID visitId) {
        URI baseUrl = webApplicationProperties.baseUrl();
        return UriComponentsBuilder.fromUri(baseUrl)
                .replacePath(null)
                .replaceQuery(null)
                .fragment(null)
                .pathSegment("clinic", "consultation", visitId.toString())
                .build()
                .toUri();
    }
}
