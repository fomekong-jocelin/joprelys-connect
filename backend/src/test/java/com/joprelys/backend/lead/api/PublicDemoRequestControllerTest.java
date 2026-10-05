package com.joprelys.backend.lead.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.joprelys.backend.lead.domain.DemoRequest;
import com.joprelys.backend.lead.domain.DemoRequestStatus;
import com.joprelys.backend.lead.dto.DemoRequestDto;
import com.joprelys.backend.lead.repository.DemoRequestRepository;
import java.util.List;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PublicDemoRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private DemoRequestRepository repository;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void prepareMimeMessages() {
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void shouldCreateDemoRequestWithoutAuthentication() throws Exception {
        DemoRequestDto dto = new DemoRequestDto(
                "Dr. Jocelin Fomekong",
                "Clinique Sainte-Anne Douala",
                "Directeur Médical",
                "+237 691893198",
                "jocelin@clinique-ste-anne.cm",
                "Douala",
                "Déploiement DPU et IA vocale aux urgences",
                "landing-page",
                "fr"
        );

        mockMvc.perform(post("/api/public/demo-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.fullName", is("Dr. Jocelin Fomekong")))
                .andExpect(jsonPath("$.organizationName", is("Clinique Sainte-Anne Douala")))
                .andExpect(jsonPath("$.status", is("NEW")));

        List<DemoRequest> saved = repository.findAll();
        assertThat(saved).anyMatch(r ->
                r.getFullName().equals("Dr. Jocelin Fomekong")
                        && r.getOrganizationName().equals("Clinique Sainte-Anne Douala")
                        && r.getPhone().equals("+237 691893198")
                        && r.getStatus() == DemoRequestStatus.NEW
        );
    }

    @Test
    void shouldRejectDemoRequestWhenRequiredFieldsAreMissing() throws Exception {
        DemoRequestDto dto = new DemoRequestDto(
                "",
                "",
                "Directeur",
                "",
                "invalid-email",
                "Douala",
                "test",
                "landing-page",
                "fr"
        );

        mockMvc.perform(post("/api/public/demo-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotifyTheConfiguredContactOnlyAfterCommit() throws Exception {
        UUID id = submitValidRequest();
        verify(mailSender, never()).send(any(MimeMessage.class));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        try {
            ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
            verify(mailSender).send(message.capture());
            assertThat(message.getValue().getAllRecipients()).extracting(Object::toString).containsExactly("contact@joprelys.com");
            assertThat(new MimeMessageHelper(message.getValue()).getMimeMessage().getSubject()).contains("Joprelys Connect");
            assertThat(repository.findById(id)).get().extracting(DemoRequest::getFullName).isEqualTo("Demo Test");
            assertThat(repository.findById(id)).isPresent();
        } finally {
            repository.deleteById(id);
        }
    }

    @Test
    void shouldKeepTheRequestWhenSmtpFails() throws Exception {
        doThrow(new MailSendException("Simulated SMTP failure")).when(mailSender).send(any(MimeMessage.class));
        UUID id = submitValidRequest();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        try {
            verify(mailSender).send(any(MimeMessage.class));
            assertThat(repository.findById(id)).isPresent();
        } finally {
            repository.deleteById(id);
        }
    }

    @Test
    void shouldNeverNotifyOnRollback() throws Exception {
        UUID id = submitValidRequest();
        TestTransaction.flagForRollback();
        TestTransaction.end();
        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void shouldRejectAnInvalidOptionalEmail() throws Exception {
        DemoRequestDto dto = new DemoRequestDto("Demo Test", "Clinic Test", null, "+237600000000",
                "invalid-email", null, null, null, "en");
        mockMvc.perform(post("/api/public/demo-requests").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))).andExpect(status().isBadRequest());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private UUID submitValidRequest() throws Exception {
        DemoRequestDto dto = new DemoRequestDto("Demo Test", "Clinic Test", null, "+237600000000",
                null, null, null, "landing-page", "fr");
        String response = mockMvc.perform(post("/api/public/demo-requests").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }
}
