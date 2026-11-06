package es.empleofacil.lecturafacil;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(EasyReadController.class)
class EasyReadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EasyReadService easyReadService;

    @Test
    void adaptReturnsReportWithTextChangesReadabilityAndViolations() throws Exception {
        when(easyReadService.adapt("Se precisa el CAP.")).thenReturn(new EasyReadResult("Necesitas el CAP.",
                List.of(new Change("CAP", "CAP", "Revisar: no conozco esta sigla."))));

        postText("Se precisa el CAP.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adaptedText").value("Necesitas el CAP."))
                .andExpect(jsonPath("$.changes[0].reason").value("Revisar: no conozco esta sigla."))
                .andExpect(jsonPath("$.readabilityBefore.words").value(4))
                .andExpect(jsonPath("$.readabilityAfter.inflesz.level").exists())
                .andExpect(jsonPath("$.violations[0].rule").value("ACRONYM"))
                .andExpect(jsonPath("$.violations[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.violations[0].fragment").value("CAP"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "   ", " ... " })
    void textWithoutWordsReturnsBadRequestWithoutCallingTheAi(String text) throws Exception {
        postText(text)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Escribe un texto para adaptar."));

        verifyNoInteractions(easyReadService);
    }

    @Test
    void tooLongTextReturnsBadRequestWithoutCallingTheAi() throws Exception {
        postText("a".repeat(EasyReadRequest.MAX_LENGTH + 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.startsWith("El texto es muy largo.")));

        verifyNoInteractions(easyReadService);
    }

    @Test
    void unreadableJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/easy-read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("esto no es JSON"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.startsWith("No entendemos lo que has enviado.")));
    }

    @Test
    void aiFailureReturnsServiceUnavailableWithEasyReadMessage() throws Exception {
        when(easyReadService.adapt(anyString())).thenThrow(new EasyReadException("Ollama apagado"));

        postText("Texto difícil")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("No se puede adaptar el texto ahora"))
                .andExpect(jsonPath("$.detail").value(Matchers.startsWith("Ahora no podemos adaptar el texto.")));
    }

    private ResultActions postText(String text) throws Exception {
        return mockMvc.perform(post("/api/easy-read")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"" + text + "\"}"));
    }
}
