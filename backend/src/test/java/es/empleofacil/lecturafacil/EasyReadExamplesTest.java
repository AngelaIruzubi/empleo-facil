package es.empleofacil.lecturafacil;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.io.DefaultResourceLoader;

import es.empleofacil.lecturafacil.analysis.RuleChecker;
import es.empleofacil.lecturafacil.analysis.Severity;

class EasyReadExamplesTest {

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
        // Los avisos se permiten: por ejemplo, la sigla DNI ya está explicada en la misma frase
        assertThat(RuleChecker.check(examples.adapted(name)))
                .filteredOn(violation -> violation.severity() == Severity.ERROR)
                .isEmpty();
    }
}
