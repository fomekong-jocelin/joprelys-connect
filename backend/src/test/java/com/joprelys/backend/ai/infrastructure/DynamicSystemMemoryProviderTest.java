package com.joprelys.backend.ai.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.infrastructure.claude.ClaudeProvider;
import com.joprelys.backend.ai.infrastructure.gemini.GeminiProvider;
import com.joprelys.backend.ai.infrastructure.openai.OpenAiProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DynamicSystemMemoryProviderTest {

    @Test
    void shouldMergeDynamicMemoryIntoOpenAiSystemPrompt() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProvider provider = new OpenAiProvider(
                builder.build(),
                new AiProperties.OpenAiProperties(
                        "key", "gpt-4.1", "stt", null, "https://openai.test/v1"));

        server.expect(requestTo("https://openai.test/v1/chat/completions"))
                .andExpect(content().json("""
                        {
                          "model":"gpt-4.1",
                          "temperature":0.3,
                          "messages":[
                            {"role":"system","content":"BASE\n\nMEMORY"},
                            {"role":"user","content":"Current turn"}
                          ]
                        }
                        """))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"{}"}}],"model":"gpt-4.1"}
                        """, MediaType.APPLICATION_JSON));

        var response = provider.chat(messages(), "BASE");

        assertEquals("{}", response.content());
        server.verify();
    }

    @Test
    void shouldMergeDynamicMemoryIntoGeminiSystemInstruction() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://gemini.test/v1beta");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiProvider provider = new GeminiProvider(
                builder.build(),
                new AiProperties.GeminiProperties(
                        "key", "gemini-test", "https://gemini.test/v1beta"));

        server.expect(requestTo(
                        "https://gemini.test/v1beta/models/gemini-test:generateContent?key=key"))
                .andExpect(content().json("""
                        {
                          "contents":[{
                            "role":"user",
                            "parts":[{"text":"Current turn"}]
                          }],
                          "systemInstruction":{
                            "parts":[{"text":"BASE\n\nMEMORY"}]
                          },
                          "generationConfig":{
                            "temperature":0.3,
                            "responseMimeType":"text/plain"
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{}"}]}}],"usageMetadata":{}}
                        """, MediaType.APPLICATION_JSON));

        var response = provider.chat(messages(), "BASE");

        assertEquals("{}", response.content());
        server.verify();
    }

    @Test
    void shouldMergeDynamicMemoryIntoClaudeSystemPrompt() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://claude.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeProvider provider = new ClaudeProvider(
                builder.build(),
                new AiProperties.ClaudeProperties(
                        "key", "claude-test", "https://claude.test/v1"),
                null,
                null);

        server.expect(requestTo("https://claude.test/v1/messages"))
                .andExpect(content().json("""
                        {
                          "model":"claude-test",
                          "max_tokens":4096,
                          "system":"BASE\n\nMEMORY",
                          "messages":[{"role":"user","content":"Current turn"}]
                        }
                        """))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"{}"}],"model":"claude-test","usage":{}}
                        """, MediaType.APPLICATION_JSON));

        var response = provider.chat(messages(), "BASE");

        assertEquals("{}", response.content());
        server.verify();
    }

    private List<AiMessage> messages() {
        return List.of(
                AiMessage.system("MEMORY"),
                AiMessage.user("Current turn"));
    }
}
