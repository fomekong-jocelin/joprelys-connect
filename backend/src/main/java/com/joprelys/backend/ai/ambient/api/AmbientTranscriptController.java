package com.joprelys.backend.ai.ambient.api;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptLedgerView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptionService;
import com.joprelys.backend.ai.ambient.application.DoctorSpeakerReferenceFactory;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/consultations/{visitId}/ambient")
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AmbientTranscriptController {

    private final AmbientTranscriptionService transcriptionService;
    private final AmbientTranscriptLedgerService ledgerService;
    private final DoctorSpeakerReferenceFactory doctorSpeakerReferenceFactory;

    public AmbientTranscriptController(
            AmbientTranscriptionService transcriptionService,
            AmbientTranscriptLedgerService ledgerService,
            DoctorSpeakerReferenceFactory doctorSpeakerReferenceFactory) {
        this.transcriptionService = transcriptionService;
        this.ledgerService = ledgerService;
        this.doctorSpeakerReferenceFactory = doctorSpeakerReferenceFactory;
    }

    @PostMapping(
            value = "/transcriptions/audio",
            consumes = {"audio/webm", "audio/mp4", "audio/mpeg", "audio/wav", "audio/ogg", "audio/flac"})
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public List<TranscriptItemView> ingestAudioChunk(
            @PathVariable UUID visitId,
            @RequestBody byte[] audio,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
            @RequestHeader("X-Joprelys-Ambient-Chunk-Id") String chunkId,
            @RequestHeader(value = "X-Joprelys-Ambient-Start-Ms", defaultValue = "0") long startOffsetMs,
            @RequestHeader(value = "X-Joprelys-Locale", defaultValue = "fr") String locale,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return transcriptionService.ingestAudioChunk(
                visitId,
                identity.userId(),
                identity.organizationId(),
                chunkId,
                startOffsetMs,
                locale,
                audio,
                contentType);
    }

    @PostMapping(
            value = "/transcriptions/audio-with-doctor-reference",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public List<TranscriptItemView> ingestAudioChunkWithDoctorReference(
            @PathVariable UUID visitId,
            @RequestPart("audio") MultipartFile audio,
            @RequestPart("doctorReference") MultipartFile doctorReference,
            @RequestHeader("X-Joprelys-Ambient-Chunk-Id") String chunkId,
            @RequestHeader(value = "X-Joprelys-Ambient-Start-Ms", defaultValue = "0") long startOffsetMs,
            @RequestHeader(value = "X-Joprelys-Locale", defaultValue = "fr") String locale,
            Authentication authentication) {
        Identity identity = identity(authentication);
        try {
            byte[] audioBytes = audio.getBytes();
            byte[] referenceBytes = doctorReference.getBytes();
            var doctorReferenceValue = doctorSpeakerReferenceFactory.create(
                    referenceBytes,
                    doctorReference.getContentType());
            return transcriptionService.ingestAudioChunk(
                    visitId,
                    identity.userId(),
                    identity.organizationId(),
                    chunkId,
                    startOffsetMs,
                    locale,
                    audioBytes,
                    audio.getContentType(),
                    List.of(doctorReferenceValue));
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI_AMBIENT_MULTIPART_AUDIO_INVALID",
                    exception);
        }
    }

    @PostMapping("/transcript/{itemId}/corrections")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public TranscriptItemView correctTranscriptItem(
            @PathVariable UUID visitId,
            @PathVariable UUID itemId,
            @Valid @RequestBody TranscriptCorrectionRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return ledgerService.appendCorrection(
                visitId,
                itemId,
                identity.userId(),
                identity.organizationId(),
                request.correctionId(),
                request.speakerType(),
                request.text());
    }

    @GetMapping("/transcript")
    @PreAuthorize("hasAuthority('CLINICAL_READ') or hasAuthority('CLINICAL_WRITE')")
    public TranscriptLedgerView transcript(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return ledgerService.listFinal(visitId, identity.organizationId());
    }

    @GetMapping("/transcript/audit")
    @PreAuthorize("hasAuthority('CLINICAL_READ') or hasAuthority('CLINICAL_WRITE')")
    public TranscriptLedgerView transcriptAudit(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return ledgerService.listAudit(visitId, identity.organizationId());
    }

    private Identity identity(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof JwtClaims claims)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        UUID userId = parseUuid(claims.subject())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "AUTH_IDENTITY_UNRESOLVED"));
        UUID organizationId = Optional.ofNullable(TenantContext.getTenantId())
                .or(() -> parseUuid(claims.organizationId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "AUTH_TENANT_UNRESOLVED"));
        return new Identity(userId, organizationId);
    }

    private Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public record TranscriptCorrectionRequest(
            @NotBlank @Size(max = 150)
            @Pattern(regexp = "[A-Za-z0-9._:-]+") String correctionId,
            @NotBlank @Pattern(regexp = "DOCTOR|PATIENT") String speakerType,
            @Size(max = 12000) String text) {
    }

    private record Identity(UUID userId, UUID organizationId) {
    }
}
