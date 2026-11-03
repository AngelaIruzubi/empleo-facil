package es.empleofacil.lecturafacil;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.io.DefaultResourceLoader;

class EasyReadExamplesTest {

    private static final int MAX_WORDS_PER_LINE = 15;
    private static final Pattern FORBIDDEN_SIGNS = Pattern.compile("[;()\\[\\]%&$#/§]|\\.\\.\\.|…");
    private static final Pattern MENTE_WORD = Pattern.compile("\\p{L}+mente\\b");
    private static final Pattern ORDINAL = Pattern.compile("\\d+\\.?[ºª]");

    static final List<String> names = EasyReadExamples.NAMES;

    private final EasyReadExamples examples = new EasyReadExamples(new DefaultResourceLoader());

    @Test
    void loadsOneUserAndOneAssistantMessagePerExample() {
        List<Message> messages = examples.messages();

        assertThat(messages).hasSize(names.size() * 2);
        for (int i = 0; i < messages.size(); i += 2) {
            assertThat(messages.get(i)).isInstanceOf(UserMessage.class);
            assertThat(messages.get(i + 1)).isInstanceOf(AssistantMessage.class);
        }
    }

    @ParameterizedTest
    @FieldSource("names")
    void exampleAnswerIsValidJsonWithTextAndChanges(String name) {
        EasyReadResult result = examples.result(name);

        assertThat(result.adaptedText()).isEqualTo(examples.adapted(name));
        assertThat(result.changes()).isNotEmpty().allSatisfy(change -> {
            assertThat(change.original()).isNotBlank();
            assertThat(change.adapted()).isNotBlank();
            assertThat(change.reason()).isNotBlank();
        });
    }

    // El modelo copia los ejemplos, así que tienen que cumplir nuestras reglas
    @ParameterizedTest
    @FieldSource("names")
    void adaptedExampleFollowsEasyReadRules(String name) {
        for (String line : examples.adapted(name).lines().toList()) {
            String text = line.replaceFirst("^- ", "").strip();
            if (text.isEmpty()) {
                continue;
            }
            assertThat(text.split("\\s+")).as("Línea demasiado larga: %s", line).hasSizeLessThanOrEqualTo(MAX_WORDS_PER_LINE);
            assertThat(FORBIDDEN_SIGNS.matcher(text).find()).as("Signo prohibido en: %s", line).isFalse();
            assertThat(MENTE_WORD.matcher(text).find()).as("Palabra en -mente en: %s", line).isFalse();
            assertThat(ORDINAL.matcher(text).find()).as("Número ordinal en: %s", line).isFalse();
        }
    }
}
