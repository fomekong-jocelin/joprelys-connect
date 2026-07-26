package com.joprelys.backend.ai.api;

import com.joprelys.backend.ai.infrastructure.openai.OpenAiRealtimeCallService;
import com.joprelys.backend.visit.application.VisitService;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
@PreAuthorize("hasAuthority('VISIT_VITALS_WRITE')")
public class AiRealtimeVitalsController {

    private static final MediaType APPLICATION_SDP = MediaType.valueOf("application/sdp");

    private final VisitService visitService;
    private final OpenAiRealtimeCallService realtimeCallService;

    public AiRealtimeVitalsController(
            VisitService visitService,
            OpenAiRealtimeCallService realtimeCallService) {
        this.visitService = visitService;
        this.realtimeCallService = realtimeCallService;
    }

    @PostMapping(
            value = "/api/ai/realtime/vitals/{visitId}/calls",
            consumes = "application/sdp",
            produces = "application/sdp")
    public ResponseEntity<String> createCall(
            @PathVariable UUID visitId,
            @RequestParam(defaultValue = "fr") String locale,
            @RequestBody String sdp) {
        var visit = visitService.getVisit(visitId);
        if (!"EN_COURS".equals(visit.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "VISIT_NOT_ACTIVE");
        }
        String answer = realtimeCallService.createCall(sdp, locale);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .contentType(APPLICATION_SDP)
                .body(answer);
    }
}
