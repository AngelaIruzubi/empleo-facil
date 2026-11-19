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

// Test unitario: sin Spring, creamos el servicio con new
class RevisionServiceTest {

    private final Resource systemPrompt = new ClassPathResource("prompts/revise-system.st");
    private final ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    private final RevisionService service = new RevisionService(chatClient, systemPrompt);

    private final Correction hours = new Correction("Trabajas de lunes a viernes.", "Falta que son 20 horas.");
    private final Correction extraPay = new Correction("Las pagas son dinero extra.", "La paga extra ya está dentro del sueldo.");
    private final ReviseRequest request = new ReviseRequest(
            "Jornada parcial de 20 horas. El sueldo incluye la paga extra.",
            "Trabajas de lunes a viernes.\nLas pagas son dinero extra.",
            List.of(hours, extraPay));

    @Test
    void reviseAsksTheAiOnceForEachMarkedSentenceAndKeepsTheOrder() {
        when(answer(hours)).thenReturn(answerWith("Trabajas 20 horas cada semana."));
        when(answer(extraPay)).thenReturn(answerWith(" El sueldo ya incluye la paga extra. "));

        assertThat(service.revise(request)).containsExactly(
                "Trabajas 20 horas cada semana.",
                "El sueldo ya incluye la paga extra.");
    }

    @Test
    void severalSentencesGoEachOnItsOwnLineInThePlaceOfTheMarkedSentence() {
        // Lo que pasó el 18 y el 19/11/2026: 1 frase marcada y la IA contestó con varias frases
        when(answer(hours)).thenReturn(answerWith(
                "Trabajas 20 horas cada semana. Trabajas de lunes a viernes.\nEs media jornada."));
        when(answer(extraPay)).thenReturn(answerWith("El sueldo ya incluye la paga extra."));

        assertThat(service.revise(request)).containsExactly(
                "Trabajas 20 horas cada semana.\nTrabajas de lunes a viernes.\nEs media jornada.",
                "El sueldo ya incluye la paga extra.");
    }

    @Test
    void userMessageHasTheOfferTheWholeTextAndAtTheEndTheSentenceAndTheComment() {
        String message = RevisionService.userMessage(request, extraPay);

        assertThat(message).containsSubsequence(
                "OFERTA ORIGINAL:", "Jornada parcial de 20 horas.",
                "TEXTO ADAPTADO COMPLETO:", "Trabajas de lunes a viernes.\nLas pagas son dinero extra.",
                "FRASE QUE HAY QUE CORREGIR:", "Las pagas son dinero extra.",
                "QUÉ ESTÁ MAL, SEGÚN EL PREPARADOR LABORAL:", "La paga extra ya está dentro del sueldo.");
        // El principio es igual para todas las frases: Ollama no tiene que volver a leerlo
        String start = message.substring(0, message.indexOf("FRASE QUE HAY QUE CORREGIR:"));
        assertThat(RevisionService.userMessage(request, hours)).startsWith(start);
    }

    @Test
    void answerWithoutWordsBecomesRevisionException() {
        when(answer(hours)).thenReturn(answerWith("..."));

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

    private ResponseEntity<ChatResponse, RevisionAnswer> answer(Correction correction) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(RevisionService.userMessage(request, correction))
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .call()
                .responseEntity(RevisionAnswer.class);
    }

    private static ResponseEntity<ChatResponse, RevisionAnswer> answerWith(String correctedText) {
        return new ResponseEntity<>(null, new RevisionAnswer(correctedText));
    }
}
