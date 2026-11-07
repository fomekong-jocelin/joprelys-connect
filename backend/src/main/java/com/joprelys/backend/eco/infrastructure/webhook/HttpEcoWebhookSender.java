package com.joprelys.backend.eco.infrastructure.webhook;

import com.joprelys.backend.eco.config.EcoProperties;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** POST JSON signé ({@code Eco-Signature}) avec {@code Idempotency-Key} = identifiant de l'événement. */
@Component
public class HttpEcoWebhookSender implements EcoWebhookSender {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final RestClient restClient;
    private final Clock clock;

    public HttpEcoWebhookSender(Clock clock) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.clock = clock;
    }

    @Override
    public DeliveryResult send(EcoProperties.Subscriber subscriber, UUID eventId, String body) {
        String signature = EcoWebhookSigner.sign(subscriber.secret(), clock.instant().getEpochSecond(), body);
        try {
            HttpStatusCode status = restClient.post()
                    .uri(subscriber.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(EcoWebhookSigner.HEADER, signature)
                    .header(IDEMPOTENCY_KEY, eventId.toString())
                    .body(body)
                    .exchange((request, response) -> response.getStatusCode());
            return status.is2xxSuccessful() ? DeliveryResult.ok() : DeliveryResult.failed("HTTP " + status.value());
        } catch (RestClientException e) {
            return DeliveryResult.failed("transport: " + e.getClass().getSimpleName());
        }
    }
}
