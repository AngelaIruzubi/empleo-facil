package es.empleofacil.lecturafacil.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextSplitterTest {

    // --- Frases ---

    @Test
    void splitsSentencesByFullStopQuestionAndExclamationMarks() {
        assertThat(TextSplitter.sentences("¿Tienes coche? Lo necesitas. ¡Suerte!"))
                .containsExactly("¿Tienes coche?", "Lo necesitas.", "¡Suerte!");
    }

    @Test
    void eachLineIsASentenceEvenWithoutFullStop() {
        assertThat(TextSplitter.sentences("Trae esto:\n- Ropa cómoda\n- El DNI"))
                .containsExactly("Trae esto:", "- Ropa cómoda", "- El DNI");
    }

    @Test
    void ignoresBlankLinesBetweenParagraphs() {
        assertThat(TextSplitter.sentences("Hola.\n\n\nAdiós."))
                .containsExactly("Hola.", "Adiós.");
    }

    @Test
    void abbreviationsDoNotEndTheSentence() {
        assertThat(TextSplitter.sentences("Habla con la Sra. López. Te espera."))
                .containsExactly("Habla con la Sra. López.", "Te espera.");
    }

    @Test
    void initialsDoNotEndTheSentence() {
        assertThat(TextSplitter.sentences("Pregunta por J. García."))
                .containsExactly("Pregunta por J. García.");
    }

    @Test
    void dotsInsideNumbersAndWebsitesDoNotEndTheSentence() {
        assertThat(TextSplitter.sentences("Mira www.sepe.es antes del 1.5.2027."))
                .containsExactly("Mira www.sepe.es antes del 1.5.2027.");
    }

    @Test
    void ellipsisEndsTheSentence() {
        assertThat(TextSplitter.sentences("Espera… Ya está."))
                .containsExactly("Espera…", "Ya está.");
    }

    @Test
    void hourAbbreviationAtTheEndEndsTheSentence() {
        assertThat(TextSplitter.sentences("Entras a las 8 h. Sales a las 15 h."))
                .containsExactly("Entras a las 8 h.", "Sales a las 15 h.");
    }

    @Test
    void emptyTextHasNoSentences() {
        assertThat(TextSplitter.sentences("  \n ")).isEmpty();
    }

    // --- Palabras ---

    @Test
    void wordsIgnorePunctuationAndKeepAccents() {
        assertThat(TextSplitter.words("¡Ánimo, Begoña! ¿Cuándo empiezas?"))
                .containsExactly("Ánimo", "Begoña", "Cuándo", "empiezas");
    }

    @Test
    void numbersWithCommaOrColonAreOneWord() {
        assertThat(TextSplitter.words("Cobrarás 7,5 euros y entras a las 8:00."))
                .containsExactly("Cobrarás", "7,5", "euros", "y", "entras", "a", "las", "8:00");
    }

    @Test
    void hyphenatedWordsAreOneWord() {
        assertThat(TextSplitter.words("Centro socio-sanitario"))
                .containsExactly("Centro", "socio-sanitario");
    }

    @Test
    void emailIsOneWord() {
        assertThat(TextSplitter.words("Escribe a rrhh@empresa.es antes del lunes."))
                .containsExactly("Escribe", "a", "rrhh@empresa.es", "antes", "del", "lunes");
    }

    @Test
    void dateWithSlashesIsOneWord() {
        assertThat(TextSplitter.words("El 05/11/2026 empiezas."))
                .containsExactly("El", "05/11/2026", "empiezas");
    }

    @Test
    void listDashIsNotAWord() {
        assertThat(TextSplitter.words("- Ropa cómoda")).containsExactly("Ropa", "cómoda");
    }
}
