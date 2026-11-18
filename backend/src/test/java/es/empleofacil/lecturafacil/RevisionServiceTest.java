package es.empleofacil.lecturafacil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import es.empleofacil.lecturafacil.ReviseRequest.Correction;

// Test unitario: sin Spring, creamos el servicio con new
class RevisionServiceTest {

    private final Resource systemPrompt = new ClassPathResource("prompts/revise-system.st");
    private final ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    private final RevisionService service = new RevisionService(chatClient, systemPrompt);

    private final ReviseRequest request = new ReviseRequest(
            "Jornada parcial de 20 horas. El sueldo incluye la paga extra.",
            "Trabajas de lunes a viernes.\nLas pagas son dinero extra.",
            List.of(new Correction("Trabajas de lunes a viernes.", "Falta que son 20 horas."),
                    new Correction("Las pagas son dinero extra.", "La paga extra ya está dentro del sueldo.")));

    @Test
    void reviseSendsTheOfferTheTextAndTheNumberedCorrectionsAndReturnsTheSentences() {
        when(answer()).thenReturn(new ResponseEntity<>(null, new RevisedSentences(List.of(
                " Trabajas 20 horas cada semana.\nTrabajas de lunes a viernes. ",
                "El sueldo ya incluye la paga extra."))));

        assertThat(service.revise(request)).containsExactly(
                "Trabajas 20 horas cada semana.\nTrabajas de lunes a viernes.",
                "El sueldo ya incluye la paga extra.");
    }

    @Test
    void userMessageHasTheOfferTheWholeTextAndEachSentenceWithItsComment() {
        assertThat(RevisionService.userMessage(request)).containsSubsequence(
                "OFERTA ORIGINAL:", "Jornada parcial de 20 horas.",
                "TEXTO ADAPTADO COMPLETO:", "Las pagas son dinero extra.",
                "FRASES QUE HAY QUE CORREGIR (2):",
                "1. Frase: Trabajas de lunes a viernes.", "Qué está mal: Falta que son 20 horas.",
                "2. Frase: Las pagas son dinero extra.", "Qué está mal: La paga extra ya está dentro del sueldo.");
    }

    @Test
    void wrongNumberOfSentencesBecomesEasyReadException() {
        // Si faltan frases, no sabemos cuál va en cada sitio
        when(answer()).thenReturn(new ResponseEntity<>(null, new RevisedSentences(List.of("Solo una frase."))));

        assertThatThrownBy(() -> service.revise(request)).isInstanceOf(EasyReadException.class);
    }

    @Test
    void sentenceWithoutWordsBecomesEasyReadException() {
        when(answer()).thenReturn(new ResponseEntity<>(null, new RevisedSentences(List.of("Bien.", "..."))));

        assertThatThrownBy(() -> service.revise(request)).isInstanceOf(EasyReadException.class);
    }

    @Test
    void aiErrorBecomesEasyReadException() {
        when(chatClient.prompt().system(systemPrompt).user(anyString())
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT).call())
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> service.revise(request))
                .isInstanceOf(EasyReadException.class)
                .hasRootCauseMessage("Connection refused");
    }

    @Test
    void systemPromptFileExists() {
        assertThat(systemPrompt.exists()).isTrue();
    }

    private ResponseEntity<?, RevisedSentences> answer() {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(RevisionService.userMessage(request))
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .call()
                .responseEntity(RevisedSentences.class);
    }
}
