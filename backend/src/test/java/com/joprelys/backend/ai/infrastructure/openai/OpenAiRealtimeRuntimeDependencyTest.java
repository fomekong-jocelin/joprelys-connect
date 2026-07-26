package com.joprelys.backend.ai.infrastructure.openai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.MultipartBodyBuilder;

class OpenAiRealtimeRuntimeDependencyTest {

    @Test
    void shouldLoadReactiveStreamsPublisherRequiredBySpringMultipartBuilder() {
        assertDoesNotThrow(() -> Class.forName("org.reactivestreams.Publisher"));
    }

    @Test
    void shouldInstantiateMultipartBuilderWithoutRuntimeLinkageError() {
        assertDoesNotThrow(MultipartBodyBuilder::new);
    }
}
