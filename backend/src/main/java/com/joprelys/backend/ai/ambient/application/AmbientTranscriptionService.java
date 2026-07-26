package com.joprelys.backend.ai.ambient.application;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AmbientTranscriptionService {

    private final AmbientDiarizationPort diarizationPort;
    private final AmbientTranscriptLedgerService ledgerService;

    public AmbientTranscriptionService(
            AmbientDiarizationPort diarizationPort,
            AmbientTranscriptLedgerService ledgerService) {
        this.diarizationPort = diarizationPort;
        this.ledgerService = ledgerService;
    }

    public List<TranscriptItemView> ingestAudioChunk(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String chunkId,
            long chunkStartOffsetMs,
            String locale,
            byte[] audio,
            String contentType) {
        var diarized = diarizationPort.transcribe(audio, contentType, locale);
        return ledgerService.appendDiarizedSegments(
                visitId,
                userId,
                organizationId,
                chunkId,
                chunkStartOffsetMs,
                locale,
                diarized.segments());
    }
}
