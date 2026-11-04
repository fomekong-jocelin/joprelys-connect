package com.joprelys.backend.common.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * SEC-13 / FA-43 : les erreurs de requête du client ne doivent plus produire de 500 ni exposer
 * de détail technique ; le format d'erreur normalisé est conservé.
 */
class GlobalExceptionHandlerClientErrorTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void missingRequestParameterReturns400WithGenericMessage() throws Exception {
        mockMvc.perform(get("/probe/page"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.message")
                        .value("La requête est incomplète : un paramètre obligatoire est manquant."))
                .andExpect(jsonPath("$.detail")
                        .value("La requête est incomplète : un paramètre obligatoire est manquant."))
                .andExpect(jsonPath("$.error.trace_id").exists());
    }

    @Test
    void malformedPathVariableReturns400WithoutTechnicalDetail() throws Exception {
        mockMvc.perform(get("/probe/items/pas-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.message")
                        .value("La requête est invalide : un paramètre n'a pas le format attendu."))
                .andExpect(content().string(not(containsString("java.util.UUID"))));
    }

    @Test
    void unreadableJsonBodyReturns400WithoutParserDetail() throws Exception {
        mockMvc.perform(post("/probe/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Patient\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("Le contenu de la requête est illisible ou mal formé."))
                .andExpect(content().string(not(containsString("JSON parse error"))));
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/probe/json")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=Patient"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void oversizedUploadReturns413() throws Exception {
        mockMvc.perform(post("/probe/upload"))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.error.code").value("PAYLOAD_TOO_LARGE"))
                .andExpect(jsonPath("$.error.message").value("Le fichier envoyé dépasse la taille maximale autorisée."));
    }

    @Test
    void unsupportedHttpMethodReturns405() throws Exception {
        mockMvc.perform(get("/probe/json"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void noSuchElementNeverEchoesTheRawExceptionMessage() throws Exception {
        mockMvc.perform(get("/probe/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("La ressource demandée est introuvable."))
                .andExpect(content().string(not(containsString("No value present"))));
    }

    @RestController
    @RequestMapping("/probe")
    static class ProbeController {

        @GetMapping("/page")
        String page(@RequestParam int page) {
            return "page " + page;
        }

        @GetMapping("/items/{id}")
        String item(@PathVariable UUID id) {
            return id.toString();
        }

        @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        String json(@RequestBody ProbeBody body) {
            return body.name();
        }

        @PostMapping("/upload")
        String upload() {
            throw new MaxUploadSizeExceededException(1024);
        }

        @GetMapping("/missing")
        String missing() {
            throw new NoSuchElementException("No value present");
        }
    }

    record ProbeBody(String name) {
    }
}
