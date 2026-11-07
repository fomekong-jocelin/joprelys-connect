package com.joprelys.backend.eco.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.eco.config.EcoProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Envoi réel sur un serveur HTTP local du JDK, qui recalcule la signature comme le ferait le récepteur. */
class HttpEcoWebhookSenderTest {

    private static final String SECRET = "ci-test-eco-webhook-secret-not-for-production";
    private static final Instant NOW = Instant.parse("2026-11-07T10:05:01Z");
    private static final String BODY = "{\"specversion\":\"1.0\",\"type\":\"consent.revoked\"}";

    private final HttpEcoWebhookSender sender = new HttpEcoWebhookSender(Clock.fixed(NOW, ZoneOffset.UTC));
    private final AtomicInteger responseStatus = new AtomicInteger(204);
    private final AtomicReference<String> signature = new AtomicReference<>();
    private final AtomicReference<String> idempotencyKey = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private HttpServer server;

    @BeforeEach
    void startReceiver() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/api/eco/v1/webhooks", exchange -> {
            signature.set(exchange.getRequestHeaders().getFirst("Eco-Signature"));
            idempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(responseStatus.get(), -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopReceiver() {
        server.stop(0);
    }

    @Test
    void postsSignedBodyWithIdempotencyKey() {
        UUID eventId = UUID.randomUUID();

        EcoWebhookSender.DeliveryResult result = sender.send(subscriber(), eventId, BODY);

        assertThat(result.delivered()).isTrue();
        assertThat(receivedBody.get()).isEqualTo(BODY);
        assertThat(contentType.get()).startsWith("application/json");
        assertThat(idempotencyKey.get()).isEqualTo(eventId.toString());
        assertThat(signature.get()).isEqualTo(EcoWebhookSigner.sign(SECRET, NOW.getEpochSecond(), receivedBody.get()));
    }

    @Test
    void reportsServerErrorAsRetryableFailure() {
        responseStatus.set(503);

        EcoWebhookSender.DeliveryResult result = sender.send(subscriber(), UUID.randomUUID(), BODY);

        assertThat(result.delivered()).isFalse();
        assertThat(result.error()).isEqualTo("HTTP 503");
    }

    @Test
    void reportsUnreachableReceiverWithoutThrowing() {
        server.stop(0);

        EcoWebhookSender.DeliveryResult result = sender.send(subscriber(), UUID.randomUUID(), BODY);

        assertThat(result.delivered()).isFalse();
        assertThat(result.error()).startsWith("transport: ");
    }

    private EcoProperties.Subscriber subscriber() {
        return new EcoProperties.Subscriber(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/api/eco/v1/webhooks", SECRET);
    }
}
