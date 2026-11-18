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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import es.empleofacil.lecturafacil.ReviseRequest.Correction;
import es.empleofacil.lecturafacil.RevisionAnswer.Item;

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
    void reviseSendsTheOfferTheTextAndTheNumberedCorrectionsAndReturnsTheSentencesInOrder() {
        // La IA contesta en otro orden: el número dice a qué frase marcada corresponde cada una
        when(answer()).thenReturn(answerWith(
                new Item(2, " El sueldo ya incluye la paga extra. "),
                new Item(1, "Trabajas 20 horas cada semana.")));

        assertThat(service.revise(request)).containsExactly(
                "Trabajas 20 horas cada semana.",
                "El sueldo ya incluye la paga extra.");
    }

    @Test
    void aCorrectionSplitIntoSeveralSentencesStaysTogetherInItsPlace() {
        // Lo que pasó el 18/11/2026: 1 frase marcada y la IA la corrigió con 2 frases
        when(answer()).thenReturn(answerWith(
                new Item(1, "Trabajas 20 horas cada semana."),
                new Item(1, "Trabajas de lunes a viernes."),
                new Item(2, "El sueldo ya incluye la paga extra.")));

        assertThat(service.revise(request)).containsExactly(
                "Trabajas 20 horas cada semana.\nTrabajas de lunes a viernes.",
                "El sueldo ya incluye la paga extra.");
    }

    @Test
    void severalSentencesInOneLineGoEachOnItsOwnLine() {
        // Lo que hizo la IA con la frase del logopeda: 3 frases seguidas en la misma línea
        when(answer()).thenReturn(answerWith(
                new Item(1, "Un logopeda ayuda a las personas a hablar bien. Necesitas el Grado en Logopedia."),
                new Item(2, "El sueldo ya incluye la paga extra.")));

        assertThat(service.revise(request)).containsExactly(
                "Un logopeda ayuda a las personas a hablar bien.\nNecesitas el Grado en Logopedia.",
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
    void aMissingSentenceBecomesRevisionException() {
        // Si falta la frase 2, no sabemos qué poner en su sitio
        when(answer()).thenReturn(answerWith(new Item(1, "Trabajas 20 horas cada semana.")));

        assertThatThrownBy(() -> service.revise(request)).isInstanceOf(RevisionException.class);
    }

    @Test
    void aNumberThatDoesNotExistBecomesRevisionException() {
        when(answer()).thenReturn(answerWith(
                new Item(1, "Bien."), new Item(2, "Bien."), new Item(3, "Esta frase no se ha pedido.")));

        assertThatThrownBy(() -> service.revise(request)).isInstanceOf(RevisionException.class);
    }

    @Test
    void sentenceWithoutWordsBecomesRevisionException() {
        when(answer()).thenReturn(answerWith(new Item(1, "Bien."), new Item(2, "...")));

        assertThatThrownBy(() -> service.revise(request)).isInstanceOf(RevisionException.class);
    }

    @Test
    void aiErrorBecomesRevisionException() {
        when(chatClient.prompt().system(systemPrompt).user(anyString())
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT).call())
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> service.revise(request))
                .isInstanceOf(RevisionException.class)
                .hasRootCauseMessage("Connection refused");
    }

    @Test
    void systemPromptFileExists() {
        assertThat(systemPrompt.exists()).isTrue();
    }

    private ResponseEntity<ChatResponse, RevisionAnswer> answer() {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(RevisionService.userMessage(request))
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .call()
                .responseEntity(RevisionAnswer.class);
    }

    private static ResponseEntity<ChatResponse, RevisionAnswer> answerWith(Item... items) {
        return new ResponseEntity<>(null, new RevisionAnswer(List.of(items)));
    }
}
