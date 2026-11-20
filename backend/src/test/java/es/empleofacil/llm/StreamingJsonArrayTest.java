package es.empleofacil.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class StreamingJsonArrayTest {

    @Test
    void returnsTheListOnceWhenItIsComplete() {
        StreamingJsonArray professions = new StreamingJsonArray("professions");

        assertThat(professions.accept("{\"profess")).isEmpty();
        assertThat(professions.accept("ions\": [\"cocinero\", \"pinche")).isEmpty();
        assertThat(professions.accept(" de cocina\"], \"adaptedText\": \"Hola")).contains(List.of("cocinero", "pinche de cocina"));
        // Solo una vez
        assertThat(professions.accept(" mundo\"}")).isEmpty();
    }

    @Test
    void anEmptyListIsAlsoAnAnswer() {
        StreamingJsonArray professions = new StreamingJsonArray("professions");

        assertThat(professions.accept("{\"professions\": [], \"adaptedText\": \"")).contains(List.of());
    }

    @Test
    void aBracketOrAQuoteInsideATextDoesNotCloseTheList() {
        StreamingJsonArray professions = new StreamingJsonArray("professions");

        assertThat(professions.accept("{\"professions\": [\"a]b\", \"dice \\\"hola\\\"\"")).isEmpty();
        assertThat(professions.accept("]}")).contains(List.of("a]b", "dice \"hola\""));
    }

    @Test
    void aListThatIsNotTextsIsIgnored() {
        StreamingJsonArray professions = new StreamingJsonArray("professions");

        assertThat(professions.accept("{\"professions\": [{\"name\": \"x\"}]}")).isEmpty();
    }
}
