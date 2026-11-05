package es.empleofacil.lecturafacil.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SyllableCounterTest {

    // --- Paso 1: vocales ---

    @ParameterizedTest
    @ValueSource(chars = { 'a', 'e', 'i', 'o', 'u', 'á', 'é', 'í', 'ó', 'ú', 'ü' })
    void recognisesVowels(char letter) {
        assertThat(SyllableCounter.isVowel(letter)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(chars = { 'b', 'c', 'ñ', 'z', 'h', '3', '-' })
    void otherCharactersAreNotVowels(char letter) {
        assertThat(SyllableCounter.isVowel(letter)).isFalse();
    }

    // --- Sílabas ---

    @ParameterizedTest
    @CsvSource({
            // Palabras normales
            "sol, 1", "casa, 2", "contrato, 3", "entrevista, 4", "CASA, 2",
            // Diptongos: dos vocales en una sílaba
            "puerta, 2", "ciudad, 2", "agua, 2", "camión, 2",
            // Hiatos: dos vocales en sílabas distintas
            "día, 2", "país, 2", "poeta, 3", "empleo, 3", "María, 3", "leía, 3", "aeropuerto, 5",
            // La h separa las vocales
            "ahora, 3",
            // La u que no suena y la ü que sí suena
            "queso, 2", "guitarra, 3", "pingüino, 3",
            // La y
            "y, 1", "hoy, 1", "rey, 1", "ayuda, 3", "buey, 1", "Uruguay, 3",
            // Sin vocales
            "8:00, 1"
    })
    void countsSyllables(String word, int expected) {
        assertThat(SyllableCounter.count(word)).isEqualTo(expected);
    }
}
