package es.empleofacil.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class StreamingJsonFieldTest {

    @Test
    void returnsTheFieldTextAsItArrives() {
        List<String> pieces = feed(new StreamingJsonField("adaptedText"),
                "{\"adapted", "Text\": \"Busca", "mos a alguien", ".\", \"changes\": []}");

        assertThat(pieces).containsExactly("", "Busca", "mos a alguien", ".");
    }

    @Test
    void decodesEscapesEvenWhenAChunkCutsThemInHalf() {
        List<String> pieces = feed(new StreamingJsonField("adaptedText"),
                "{\"adaptedText\":\"Hola\\", "nPon \\\"sí\\\" o \\u00", "f1o.\"}");

        assertThat(String.join("", pieces)).isEqualTo("Hola\nPon \"sí\" o ño.");
        assertThat(pieces.get(0)).isEqualTo("Hola");
    }

    @Test
    void ignoresEverythingAfterTheField() {
        List<String> pieces = feed(new StreamingJsonField("adaptedText"),
                "{\"adaptedText\": \"Fin.\", \"changes\": [{\"before\": \"x\"}]}", " más texto");

        assertThat(pieces).containsExactly("Fin.", "");
    }

    @Test
    void findsTheFieldWhenItIsNotFirst() {
        List<String> pieces = feed(new StreamingJsonField("adaptedText"),
                "{\"changes\": [], \"adaptedText\": \"Hola\"}");

        assertThat(pieces).containsExactly("Hola");
    }

    private static List<String> feed(StreamingJsonField field, String... chunks) {
        List<String> pieces = new ArrayList<>();
        for (String chunk : chunks) {
            pieces.add(field.accept(chunk));
        }
        return pieces;
    }
}
