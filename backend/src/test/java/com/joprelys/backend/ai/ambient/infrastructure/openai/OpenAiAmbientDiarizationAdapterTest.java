package com.joprelys.backend.ai.ambient.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiAmbientDiarizationAdapterTest {

    @Test
    void shouldRequestDiarizedJsonAndNormalizeRegionalLocale() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiAmbientDiarizationAdapter adapter = new OpenAiAmbientDiarizationAdapter(
                builder.build(), "gpt-4o-transcribe-diarize");

        server.expect(requestTo("https://api.openai.test/v1/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("diarized_json")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("chunking_strategy")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auto")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("fr")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("fr-FR"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("logprobs"))))
                .andRespond(withSuccess("""
                        {
                          "text":"Bonjour. Depuis trois jours.",
                          "segments":[
                            {"id":"seg-1","start":0.2,"end":1.1,"text":"Bonjour.","speaker":"A"},
                            {"id":"seg-2","start":1.3,"end":2.8,"text":"Depuis trois jours.","speaker":"B"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = adapter.transcribe(
                "audio".getBytes(StandardCharsets.UTF_8),
                "audio/webm;codecs=opus",
                "fr-FR");

        assertThat(response.segments()).hasSize(2);
        assertThat(response.segments().get(0).sourceSegmentId()).isEqualTo("seg-1");
        assertThat(response.segments().get(0).speakerLabel()).isEqualTo("A");
        assertThat(response.segments().get(0).startSeconds()).isEqualTo(0.2);
        assertThat(response.segments().get(1).speakerLabel()).isEqualTo("B");
        server.verify();
    }
}
