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
    void adaptReturnsAdaptedTextAndChanges() throws Exception {
        when(easyReadService.adapt("Texto difícil")).thenReturn(new EasyReadResult("Texto fácil",
                List.of(new Change("CAP", "CAP", "Revisar: no conozco esta sigla."))));

        postText("Texto difícil")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adaptedText").value("Texto fácil"))
                .andExpect(jsonPath("$.changes[0].original").value("CAP"))
                .andExpect(jsonPath("$.changes[0].reason").value("Revisar: no conozco esta sigla."));
    }

    @Test
    void emptyTextReturnsBadRequestWithoutCallingTheAi() throws Exception {
        postText("   ")
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
