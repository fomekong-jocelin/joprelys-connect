package com.joprelys.backend.ai.ambient.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort.KnownSpeakerReference;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class OpenAiAmbientKnownSpeakerReferenceTest {

    @Test
    void shouldSendKnownDoctorNameAndDataUrlAndPreserveReturnedSpeakerLabel() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiAmbientDiarizationAdapter adapter = new OpenAiAmbientDiarizationAdapter(
                builder.build(), "gpt-4o-transcribe-diarize");
        byte[] doctorAudio = "doctor-reference".getBytes(StandardCharsets.UTF_8);
        String expectedDataUrl = "data:audio/wav;base64,"
                + Base64.getEncoder().encodeToString(doctorAudio);

        server.expect(requestTo("https://api.openai.test/v1/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("known_speaker_names[]")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("doctor")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("known_speaker_references[]")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(expectedDataUrl)))
                .andRespond(withSuccess("""
                        {
                          "text":"Bonjour.",
                          "segments":[
                            {"id":"seg-1","start":0.0,"end":1.0,"text":"Bonjour.","speaker":"doctor"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = adapter.transcribe(
                "ambient-audio".getBytes(StandardCharsets.UTF_8),
                "audio/wav",
                "fr",
                List.of(new KnownSpeakerReference("doctor", doctorAudio, "audio/wav")));

        assertThat(response.segments()).hasSize(1);
        assertThat(response.segments().getFirst().speakerLabel()).isEqualTo("doctor");
        server.verify();
    }

    @Test
    void shouldRejectMoreThanFourKnownSpeakersBeforeNetworkCall() {
        OpenAiAmbientDiarizationAdapter adapter = new OpenAiAmbientDiarizationAdapter(
                RestClient.builder().baseUrl("https://api.openai.test/v1").build(),
                "gpt-4o-transcribe-diarize");
        List<KnownSpeakerReference> references = List.of(
                reference("one"),
                reference("two"),
                reference("three"),
                reference("four"),
                reference("five"));

        assertThatThrownBy(() -> adapter.transcribe(
                new byte[]{1}, "audio/wav", "fr", references))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_KNOWN_SPEAKER_LIMIT_EXCEEDED");
    }

    @Test
    void shouldRejectDuplicateOrUnsafeKnownSpeakerNames() {
        OpenAiAmbientDiarizationAdapter adapter = new OpenAiAmbientDiarizationAdapter(
                RestClient.builder().baseUrl("https://api.openai.test/v1").build(),
                "gpt-4o-transcribe-diarize");

        assertThatThrownBy(() -> adapter.transcribe(
                new byte[]{1},
                "audio/wav",
                "fr",
                List.of(reference("doctor"), reference("DOCTOR"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_KNOWN_SPEAKER_INVALID");

        assertThatThrownBy(() -> adapter.transcribe(
                new byte[]{1},
                "audio/wav",
                "fr",
                List.of(reference("doctor name"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_KNOWN_SPEAKER_INVALID");
    }

    private KnownSpeakerReference reference(String name) {
        return new KnownSpeakerReference(name, new byte[]{1, 2, 3}, "audio/wav");
    }
}
