package com.joprelys.backend.cash;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.cash.api.CashMovementRequest;
import com.joprelys.backend.cash.api.CashSessionResponse;
import com.joprelys.backend.cash.api.CloseSessionRequest;
import com.joprelys.backend.cash.api.OpenSessionRequest;
import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterEntity;
import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CashSessionHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CashRegisterRepository cashRegisterRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private OrganizationEntity organization;
    private UserAccountEntity cashier;
    private CashRegisterEntity register;
    private String cashierToken;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        organization = organizationRepository.save(new OrganizationEntity(
                "Clinique Clôture " + suffix,
                "closeout-" + suffix + "@joprelys.local",
                "670000010",
                "Akwa",
                "Douala"
        ));

        TenantContext.setTenantId(organization.getId());
        cashier = new UserAccountEntity(
                "cashier-closeout-" + suffix + "@joprelys.local",
                "Caissier Clôture",
                "CAISSIER",
                "passhash"
        );
        cashier.setOrganizationId(organization.getId());
        cashier = userAccountRepository.save(cashier);
        cashierToken = jwtService.createToken(cashier).value();

        register = new CashRegisterEntity("CAISSE-" + suffix.toUpperCase(), "Caisse Test Clôture");
        register.setOrganizationId(organization.getId());
        register = cashRegisterRepository.save(register);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldListOwnClosedSessionAndDownloadPdf() throws Exception {
        CashSessionResponse session = openSession(cashierToken, 10_000d);
        addCashMovement(cashierToken, 5_000d);
        closeSession(cashierToken, 15_000d, null);

        mockMvc.perform(get("/api/cash-registers/sessions/mine")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(session.id().toString()))
                .andExpect(jsonPath("$[0].status").value("CLOSED"))
                .andExpect(jsonPath("$[0].openedByName").value("Caissier Clôture"))
                .andExpect(jsonPath("$[0].reportNumber").value(startsWith("CLS-")))
                .andExpect(jsonPath("$[0].cashReceipts").value(5_000d))
                .andExpect(jsonPath("$[0].expectedCash").value(15_000d))
                .andExpect(jsonPath("$[0].declaredBalance").value(15_000d))
                .andExpect(jsonPath("$[0].discrepancyAmount").value(0d));

        mockMvc.perform(get("/api/cash-registers/sessions/" + session.id() + "/closeout-report")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"BORDEREAU_CLOTURE_CLS-")))
                .andExpect(result -> {
                    byte[] bytes = result.getResponse().getContentAsByteArray();
                    String signature = new String(bytes, 0, Math.min(bytes.length, 5), StandardCharsets.US_ASCII);
                    if (!signature.startsWith("%PDF-")) {
                        throw new AssertionError("Le bordereau généré n'est pas un PDF valide");
                    }
                });
    }

    @Test
    void shouldRejectAnotherCashierFromCloseoutReport() throws Exception {
        CashSessionResponse session = openSession(cashierToken, 20_000d);
        closeSession(cashierToken, 20_000d, null);

        TenantContext.setTenantId(organization.getId());
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserAccountEntity otherCashier = new UserAccountEntity(
                "other-cashier-" + suffix + "@joprelys.local",
                "Autre Caissier",
                "CAISSIER",
                "passhash"
        );
        otherCashier.setOrganizationId(organization.getId());
        otherCashier = userAccountRepository.save(otherCashier);
        String otherToken = jwtService.createToken(otherCashier).value();
        TenantContext.clear();

        mockMvc.perform(get("/api/cash-registers/sessions/" + session.id() + "/closeout-report")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectClinicalRoleFromPersonalCashHistory() throws Exception {
        TenantContext.setTenantId(organization.getId());
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserAccountEntity doctor = new UserAccountEntity(
                "doctor-closeout-" + suffix + "@joprelys.local",
                "Médecin Sans Caisse",
                "MEDECIN",
                "passhash"
        );
        doctor.setOrganizationId(organization.getId());
        doctor = userAccountRepository.save(doctor);
        String doctorToken = jwtService.createToken(doctor).value();
        TenantContext.clear();

        mockMvc.perform(get("/api/cash-registers/sessions/mine")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());
    }

    private CashSessionResponse openSession(String token, double openingBalance) throws Exception {
        OpenSessionRequest request = new OpenSessionRequest(register.getId(), openingBalance);
        String response = mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, CashSessionResponse.class);
    }

    private void addCashMovement(String token, double amount) throws Exception {
        CashMovementRequest request = new CashMovementRequest(
                "IN",
                amount,
                "Encaissement espèces de test",
                "CASH",
                "REF-CLOSEOUT",
                false
        );
        mockMvc.perform(post("/api/cash-registers/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void closeSession(String token, double declaredBalance, String reason) throws Exception {
        CloseSessionRequest request = new CloseSessionRequest(declaredBalance, reason);
        mockMvc.perform(post("/api/cash-registers/sessions/close")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }
}