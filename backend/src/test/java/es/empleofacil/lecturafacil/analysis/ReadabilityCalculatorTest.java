package es.empleofacil.lecturafacil.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReadabilityCalculatorTest {

    // Cuenta a mano: 2 frases, 6 palabras, 9 sílabas
    // Mi(1) ca-sa(2) es(1) a-zul(2). Me(1) gus-ta(2).
    // Fernández-Huerta: 206,84 − 0,60 × 150 − 1,02 × 3 = 113,78
    // INFLESZ:          206,835 − 62,3 × 1,5 − 3      = 110,385
    @Test
    void shortTextIsVeryEasy() {
        Readability readability = ReadabilityCalculator.measure("Mi casa es azul. Me gusta.");

        assertThat(readability.sentences()).isEqualTo(2);
        assertThat(readability.words()).isEqualTo(6);
        assertThat(readability.syllables()).isEqualTo(9);
        assertThat(readability.fernandezHuerta()).isEqualTo(new ReadabilityScore(113.8, "muy fácil"));
        assertThat(readability.inflesz()).isEqualTo(new ReadabilityScore(110.4, "muy fácil"));
    }

    // Principio de evals/textos/01: 3 frases, 30 palabras, 78 sílabas
    // Fernández-Huerta: 206,84 − 0,60 × 260 − 1,02 × 10 = 40,64
    // INFLESZ:          206,835 − 62,3 × 2,6 − 10      = 34,855
    @Test
    void jobOfferIsDifficult() {
        Readability readability = ReadabilityCalculator.measure("""
                Se precisa incorporar personal de reposición para centro logístico. \
                Requisitos: ESO, disponibilidad para turnos rotativos e incorporación inmediata. \
                Se valorará CAP y experiencia mínima de 6 meses en puesto similar.""");

        assertThat(readability.sentences()).isEqualTo(3);
        assertThat(readability.words()).isEqualTo(30);
        assertThat(readability.syllables()).isEqualTo(78);
        assertThat(readability.fernandezHuerta()).isEqualTo(new ReadabilityScore(40.6, "difícil"));
        assertThat(readability.inflesz()).isEqualTo(new ReadabilityScore(34.9, "muy difícil"));
    }

    @Test
    void textWithoutWordsCannotBeMeasured() {
        assertThatThrownBy(() -> ReadabilityCalculator.measure(" ... "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "39.9, muy difícil", "40, algo difícil", "54.9, algo difícil", "55, normal",
            "65, bastante fácil", "79.9, bastante fácil", "80, muy fácil"
    })
    void infleszLevels(double value, String level) {
        assertThat(ReadabilityCalculator.infleszLevel(value)).isEqualTo(level);
    }

    @ParameterizedTest
    @CsvSource({
            "29.9, muy difícil", "30, difícil", "50, bastante difícil", "60, normal",
            "70, bastante fácil", "80, fácil", "90, muy fácil"
    })
    void fernandezHuertaLevels(double value, String level) {
        assertThat(ReadabilityCalculator.fernandezHuertaLevel(value)).isEqualTo(level);
    }
}
