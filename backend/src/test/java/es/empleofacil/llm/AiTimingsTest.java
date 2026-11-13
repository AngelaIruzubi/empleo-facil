package es.empleofacil.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;

class AiTimingsTest {

    @Test
    void describesOllamaTimings() {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .keyValue("total-duration", Duration.ofMillis(43_200))
                .keyValue("load-duration", Duration.ZERO)
                .keyValue("prompt-eval-count", 2213)
                .keyValue("prompt-eval-duration", Duration.ofMillis(700))
                .keyValue("eval-count", 225)
                .keyValue("eval-duration", 42_400_000_000L)
                .build();

        assertThat(AiTimings.describe(new ChatResponse(List.of(), metadata))).isEqualTo(
                "total 43.2 s | carga 0.0 s | lectura 2213 tokens en 0.7 s | escritura 225 tokens en 42.4 s");
    }

    @Test
    void missingDataDoesNotFail() {
        assertThat(AiTimings.describe(null)).isEqualTo("sin datos de tiempo");
        assertThat(AiTimings.describe(new ChatResponse(List.of()))).isEqualTo(
                "total ? | carga ? | lectura ? tokens en ? | escritura ? tokens en ?");
    }
}
