package es.empleofacil.lecturafacil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

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
                .call()
                .entity(EasyReadResult.class))
                .thenReturn(expected);

        assertThat(service.adapt("Texto difícil")).isEqualTo(expected);
    }

    @Test
    void aiErrorBecomesEasyReadException() {
        when(chatClient.prompt()
                .system(systemPrompt)
                .messages(examples.messages())
                .user("Texto difícil")
                .call())
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> service.adapt("Texto difícil"))
                .isInstanceOf(EasyReadException.class)
                .hasRootCauseMessage("Connection refused");
    }

    @Test
    void emptyAiAnswerBecomesEasyReadException() {
        when(chatClient.prompt()
                .system(systemPrompt)
                .messages(examples.messages())
                .user("Texto difícil")
                .call()
                .entity(EasyReadResult.class))
                .thenReturn(new EasyReadResult("", List.of()));

        assertThatThrownBy(() -> service.adapt("Texto difícil"))
                .isInstanceOf(EasyReadException.class);
    }

    @Test
    void systemPromptFileExists() {
        assertThat(systemPrompt.exists()).isTrue();
    }
}
