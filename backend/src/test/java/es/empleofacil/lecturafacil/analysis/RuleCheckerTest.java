package es.empleofacil.lecturafacil.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RuleCheckerTest {

    @Test
    void easyReadTextHasNoViolations() {
        assertThat(RuleChecker.check("""
                Buscamos una persona para reponer productos.
                Vas a trabajar por las mañanas.

                ¿Qué tienes que llevar?
                - Ropa cómoda
                - Tu documento de identidad""")).isEmpty();
    }

    @Test
    void sentenceWith15WordsIsFineAndWith16IsAnError() {
        String fifteen = "uno dos tres cuatro cinco seis siete ocho nueve diez once doce trece catorce quince.";
        String sixteen = "uno dos tres cuatro cinco seis siete ocho nueve diez once doce trece catorce quince dieciséis.";

        assertThat(RuleChecker.check(fifteen)).isEmpty();
        assertThat(RuleChecker.check(sixteen))
                .extracting(RuleViolation::rule, RuleViolation::severity, RuleViolation::fragment)
                .containsExactly(tuple(EasyReadRule.LONG_SENTENCE, Severity.ERROR, "16 palabras"));
    }

    // Cada fila: frase, regla que falla y trozo que se marca
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Trabajas de 8 a 15 h (lunes a viernes).  | FORBIDDEN_SIGN    | (lunes a viernes)",
            "Trae ropa; también trae agua.            | FORBIDDEN_SIGN    | ;",
            "Espera…                                  | FORBIDDEN_SIGN    | …",
            "Cobras un 20 % más.                      | PERCENTAGE        | 20 %",
            "Es la 2.ª entrevista.                    | ORDINAL           | 2.ª",
            "Empiezas el 13/10/2026.                  | DATE_WITH_SLASHES | 13/10/2026",
            "Ven rápidamente.                         | MENTE_WORD        | rápidamente",
            "Es importantísimo.                       | ISIMO_WORD        | importantísimo",
            "Trae el DNI.                             | ACRONYM           | DNI",
            "Trae algo de comer.                      | VAGUE_WORD        | algo",
            "Ve a la oficina y/o llama.               | SLASH             | y/o"
    })
    void detectsEachRule(String sentence, EasyReadRule rule, String fragment) {
        assertThat(RuleChecker.check(sentence))
                .extracting(RuleViolation::rule, RuleViolation::fragment)
                .containsExactly(tuple(rule, fragment));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Usa tu mente.", "Cobras 7,5 euros.", "Entras a las 8:00." })
    void similarButCorrectTextsAreNotMarked(String sentence) {
        assertThat(RuleChecker.check(sentence)).isEmpty();
    }

    @Test
    void eachAcronymIsOnlyMarkedTheFirstTime() {
        List<RuleViolation> violations = RuleChecker.check("Trae el DNI.\nEl DNI es tu documento.\nY el CV.");

        assertThat(violations).extracting(RuleViolation::fragment).containsExactly("DNI", "CV");
    }

    @Test
    void violationIncludesSentenceAndAdvice() {
        RuleViolation violation = RuleChecker.check("Es la 2.ª entrevista.").getFirst();

        assertThat(violation.sentence()).isEqualTo("Es la 2.ª entrevista.");
        assertThat(violation.advice()).isEqualTo(EasyReadRule.ORDINAL.advice());
    }
}
