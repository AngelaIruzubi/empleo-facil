package es.empleofacil.lecturafacil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import reactor.core.publisher.Flux;

// Test unitario: sin Spring, creamos el servicio con new
class EasyReadServiceTest {

    private final Resource systemPrompt = new ClassPathResource("prompts/easy-read-system.st");
    private final EasyReadExamples examples = new EasyReadExamples(new DefaultResourceLoader());
    private final ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    private final EasyReadService service = new EasyReadService(chatClient, systemPrompt, examples);

    @Test
    void adaptSendsSystemPromptExamplesAndTextAndReturnsResult() {
        EasyReadResult expected = new EasyReadResult("Texto fácil",
                List.of(new Change("difícil", "fácil", "Palabra difícil cambiada.")));
        when(chatClient.prompt()
                .system(systemPrompt)
                .messages(examples.messages())
                .user("Texto difícil")
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .call()
                .responseEntity(EasyReadResult.class))
                .thenReturn(new ResponseEntity<>(null, expected));

        assertThat(service.adapt("Texto difícil")).isEqualTo(expected);
    }

    @Test
    void aiErrorBecomesEasyReadException() {
        when(chatClient.prompt()
                .system(systemPrompt)
                .messages(examples.messages())
                .user("Texto difícil")
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .call())
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> service.adapt("Texto difícil"))
                .isInstanceOf(EasyReadException.class)
                .hasRootCauseMessage("Connection refused");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ... " })
    void aiAnswerWithoutWordsBecomesEasyReadException(String adaptedText) {
        when(chatClient.prompt()
                .system(systemPrompt)
                .messages(examples.messages())
                .user("Texto difícil")
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .call()
                .responseEntity(EasyReadResult.class))
                .thenReturn(new ResponseEntity<>(null, new EasyReadResult(adaptedText, List.of())));

        assertThatThrownBy(() -> service.adapt("Texto difícil"))
                .isInstanceOf(EasyReadException.class);
    }

    @Test
    void adaptStreamingSendsTextPiecesAndReturnsTheWholeResult() {
        when(streamedChunks()).thenReturn(Flux.just(
                chunk("{\"adaptedText\": \"Busca"),
                chunk("mos a alguien.\\nEs"),
                chunk(" fácil.\", \"changes\": []}")));
        List<String> pieces = new ArrayList<>();

        EasyReadResult result = service.adaptStreaming("Texto difícil", pieces::add);

        assertThat(pieces).containsExactly("Busca", "mos a alguien.\nEs", " fácil.");
        assertThat(result).isEqualTo(new EasyReadResult("Buscamos a alguien.\nEs fácil.", List.of()));
    }

    @Test
    void adaptStreamingErrorBecomesEasyReadException() {
        when(streamedChunks()).thenReturn(Flux.concat(
                Flux.just(chunk("{\"adaptedText\": \"Hola")),
                Flux.error(new RuntimeException("Connection reset"))));

        assertThatThrownBy(() -> service.adaptStreaming("Texto difícil", piece -> { }))
                .isInstanceOf(EasyReadException.class)
                .hasRootCauseMessage("Connection reset");
    }

    @Test
    void adaptStreamingWithoutWordsBecomesEasyReadException() {
        when(streamedChunks()).thenReturn(Flux.just(chunk("{\"adaptedText\": \"...\", \"changes\": []}")));

        assertThatThrownBy(() -> service.adaptStreaming("Texto difícil", piece -> { }))
                .isInstanceOf(EasyReadException.class);
    }

    private Flux<ChatResponse> streamedChunks() {
        return chatClient.prompt()
                .system(systemPrompt)
                .messages(examples.messages())
                .user("Texto difícil")
                .options(any())
                .stream()
                .chatResponse();
    }

    private static ChatResponse chunk(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    void systemPromptFileExists() {
        assertThat(systemPrompt.exists()).isTrue();
    }
}
