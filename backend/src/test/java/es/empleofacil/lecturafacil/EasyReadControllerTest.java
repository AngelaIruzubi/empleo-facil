package es.empleofacil.lecturafacil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import es.empleofacil.pictogramas.Pictogram;
import es.empleofacil.pictogramas.ProfessionPictogram;
import es.empleofacil.pictogramas.ProfessionPictogramService;

@WebMvcTest(EasyReadController.class)
class EasyReadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EasyReadService easyReadService;

    @MockitoBean
    private ProfessionPictogramService professionPictogramService;

    @Test
    void adaptReturnsReportWithTextChangesPictogramReadabilityAndViolations() throws Exception {
        when(easyReadService.adapt("Se precisa el CAP.")).thenReturn(new EasyReadResult("Necesitas el CAP.",
                List.of(new Change("CAP", "CAP", "Revisar: no conozco esta sigla."))));
        when(professionPictogramService.find("Se precisa el CAP.")).thenReturn(new ProfessionPictogram(
                List.of("conductor"), new Pictogram(1, "conductor", "https://static.arasaac.org/pictograms/1/1_300.png")));

        postText("Se precisa el CAP.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adaptedText").value("Necesitas el CAP."))
                .andExpect(jsonPath("$.changes[0].reason").value("Revisar: no conozco esta sigla."))
                .andExpect(jsonPath("$.professions[0]").value("conductor"))
                .andExpect(jsonPath("$.pictogram.keyword").value("conductor"))
                .andExpect(jsonPath("$.pictogram.imageUrl").value("https://static.arasaac.org/pictograms/1/1_300.png"))
                .andExpect(jsonPath("$.readabilityBefore.words").value(4))
                .andExpect(jsonPath("$.readabilityAfter.inflesz.level").exists())
                .andExpect(jsonPath("$.violations[0].rule").value("ACRONYM"))
                .andExpect(jsonPath("$.violations[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.violations[0].fragment").value("CAP"));
    }

    @Test
    void reportWithoutPictogramWhenTextIsNotAJobOffer() throws Exception {
        when(easyReadService.adapt("Gracias por venir.")).thenReturn(
                new EasyReadResult("Gracias por venir.", List.of()));
        when(professionPictogramService.find("Gracias por venir.")).thenReturn(
                new ProfessionPictogram(List.of(), null));

        postText("Gracias por venir.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professions").isEmpty())
                .andExpect(jsonPath("$.pictogram").value(Matchers.nullValue()));
    }

    // Si la adaptación falla, no merece la pena gastar otra llamada a la IA en la profesión
    @Test
    void adaptationFailureDoesNotLookForTheProfession() throws Exception {
        when(easyReadService.adapt(anyString())).thenThrow(new EasyReadException("Ollama apagado"));

        postText("Texto difícil").andExpect(status().isServiceUnavailable());

        verifyNoInteractions(professionPictogramService);
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

    @Test
    void streamSendsTextPiecesAndThenTheReport() throws Exception {
        when(easyReadService.adaptStreaming(eq("Se precisa el CAP."), any())).thenAnswer(invocation -> {
            Consumer<String> onText = invocation.getArgument(1);
            onText.accept("Necesitas ");
            onText.accept("el CAP.\nYa.");
            return new EasyReadResult("Necesitas el CAP.\nYa.", List.of());
        });
        when(professionPictogramService.find("Se precisa el CAP.")).thenReturn(new ProfessionPictogram(
                List.of("conductor"), new Pictogram(1, "conductor", "https://static.arasaac.org/pictograms/1/1_300.png")));

        String body = postStream("Se precisa el CAP.");

        // Los saltos de línea van escritos como barra y n dentro del JSON, para no romper el formato del stream
        assertThat(body).containsSubsequence(
                "event:text", "data:{\"text\":\"Necesitas \"}",
                "event:text", "data:{\"text\":\"el CAP.\\nYa.\"}",
                "event:report", "\"adaptedText\":\"Necesitas el CAP.\\nYa.\"", "\"keyword\":\"conductor\"");
        assertThat(body).doesNotContain("event:error");
    }

    @Test
    void streamSendsAnErrorEventWhenTheAiFailsAndSkipsTheProfession() throws Exception {
        when(easyReadService.adaptStreaming(anyString(), any())).thenThrow(new EasyReadException("Ollama apagado"));

        String body = postStream("Texto difícil");

        assertThat(body).containsSubsequence("event:error", "\"title\":\"No se puede adaptar el texto ahora\"");
        assertThat(body).doesNotContain("event:report");
        verifyNoInteractions(professionPictogramService);
    }

    @Test
    void streamWithInvalidTextReturnsBadRequestWithoutStream() throws Exception {
        mockMvc.perform(post("/api/easy-read/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Escribe un texto para adaptar."));

        verifyNoInteractions(easyReadService);
    }

    // Espera a que el stream termine y devuelve todo lo que ha mandado
    private String postStream(String text) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/easy-read/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + text + "\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        result.getAsyncResult(5_000);
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private ResultActions postText(String text) throws Exception {
        return mockMvc.perform(post("/api/easy-read")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"" + text + "\"}"));
    }
}
