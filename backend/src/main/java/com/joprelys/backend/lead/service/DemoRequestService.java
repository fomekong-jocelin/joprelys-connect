package com.joprelys.backend.lead.service;

import com.joprelys.backend.lead.domain.DemoRequest;
import com.joprelys.backend.lead.domain.DemoRequestStatus;
import com.joprelys.backend.lead.dto.DemoRequestDto;
import com.joprelys.backend.lead.dto.DemoRequestResponse;
import com.joprelys.backend.lead.repository.DemoRequestRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoRequestService implements RegisterDemoRequest {

    private static final Logger log = LoggerFactory.getLogger(DemoRequestService.class);

    private final DemoRequestRepository repository;
    private final ApplicationEventPublisher events;

    public DemoRequestService(DemoRequestRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Override
    @Transactional
    public DemoRequestResponse registerDemoRequest(DemoRequestDto dto, HttpServletRequest httpRequest) {
        DemoRequest saved = repository.save(toLead(dto, httpRequest));
        events.publishEvent(new DemoRequestRegistered(saved.getId(), new DemoRequestDto(
                saved.getFullName(), saved.getOrganizationName(), saved.getRole(), saved.getPhone(),
                saved.getEmail(), saved.getCity(), saved.getMessage(), saved.getSource(), saved.getLocale())));
        log.info("DEMO_REQUEST_RECEIVED [id={}]", saved.getId());

        String confirmation = "fr".equalsIgnoreCase(saved.getLocale())
                ? "Votre demande de démonstration a été enregistrée avec succès. Notre équipe vous contactera dans les plus brefs délais."
                : "Your demo request has been successfully registered. Our team will contact you shortly.";

        return new DemoRequestResponse(saved.getId(), saved.getFullName(), saved.getOrganizationName(),
                saved.getStatus(), saved.getCreatedAt(), confirmation);
    }

    private DemoRequest toLead(DemoRequestDto dto, HttpServletRequest httpRequest) {
        String ip = null;
        String userAgent = null;

        if (httpRequest != null) {
            ip = httpRequest.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = httpRequest.getRemoteAddr();
            }
            userAgent = httpRequest.getHeader("User-Agent");
        }

        DemoRequest lead = new DemoRequest();
        lead.setFullName(dto.fullName().trim());
        lead.setOrganizationName(dto.organizationName().trim());
        lead.setRole(dto.role() != null ? dto.role().trim() : null);
        lead.setPhone(dto.phone().trim());
        lead.setEmail(dto.email() != null && !dto.email().isBlank() ? dto.email().trim() : null);
        lead.setCity(dto.city() != null && !dto.city().isBlank() ? dto.city().trim() : "Douala");
        lead.setMessage(dto.message() != null ? dto.message().trim() : null);
        lead.setSource(dto.source() != null && !dto.source().isBlank() ? dto.source().trim() : "landing-page");
        lead.setLocale(dto.locale() != null && !dto.locale().isBlank() ? dto.locale().trim() : "fr");
        lead.setStatus(DemoRequestStatus.NEW);
        lead.setIpAddress(ip);
        lead.setUserAgent(userAgent);

        return lead;
    }
}
