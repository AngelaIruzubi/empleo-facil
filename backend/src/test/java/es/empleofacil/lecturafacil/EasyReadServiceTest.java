package es.empleofacil.lecturafacil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

// Test unitario: sin Spring, creamos el servicio con new
class EasyReadServiceTest {

    private final Resource systemPrompt = new ClassPathResource("prompts/easy-read-system.st");
    private final ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    private final EasyReadService service = new EasyReadService(chatClient, systemPrompt);

    @Test
    void adaptSendsSystemPromptAndText() {
        when(chatClient.prompt().system(systemPrompt).user("Texto difícil").call().content())
                .thenReturn("Texto fácil");

        assertThat(service.adapt("Texto difícil")).isEqualTo("Texto fácil");
    }

    @Test
    void systemPromptFileExists() {
        assertThat(systemPrompt.exists()).isTrue();
    }
}
