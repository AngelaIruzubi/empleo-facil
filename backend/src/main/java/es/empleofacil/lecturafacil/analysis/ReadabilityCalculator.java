package es.empleofacil.lecturafacil.analysis;

import java.util.List;

// Calcula los índices de legibilidad Fernández-Huerta e INFLESZ.
// Cuanto más alta es la nota, más fácil es el texto.
// Solo miden lo largas que son las palabras y las frases: no saben si el texto se entiende.
public final class ReadabilityCalculator {

    private ReadabilityCalculator() {
    }

    public static Readability measure(String text) {
        List<String> sentences = TextSplitter.sentences(text);
        List<String> words = TextSplitter.words(text);
        if (words.isEmpty()) {
            throw new IllegalArgumentException("El texto no tiene palabras");
        }
        int syllables = words.stream().mapToInt(SyllableCounter::count).sum();

        double syllablesPerWord = (double) syllables / words.size();
        double wordsPerSentence = (double) words.size() / sentences.size();

        return new Readability(sentences.size(), words.size(), syllables,
                fernandezHuerta(syllablesPerWord, wordsPerSentence),
                inflesz(syllablesPerWord, wordsPerSentence));
    }

    // Fernández-Huerta (1959), versión corregida:
    // 206,84 − 0,60 × sílabas por cada 100 palabras − 1,02 × palabras por frase.
    // Muchas webs copian la versión original con "frases por cada 100 palabras",
    // que da más nota a las frases largas. Es una errata.
    static ReadabilityScore fernandezHuerta(double syllablesPerWord, double wordsPerSentence) {
        double value = round(206.84 - 0.60 * (syllablesPerWord * 100) - 1.02 * wordsPerSentence);
        return new ReadabilityScore(value, fernandezHuertaLevel(value));
    }

    // Szigriszt-Pazos (1993) con la escala INFLESZ de Barrio-Cantalejo (2008):
    // 206,835 − 62,3 × sílabas por palabra − palabras por frase.
    static ReadabilityScore inflesz(double syllablesPerWord, double wordsPerSentence) {
        double value = round(206.835 - 62.3 * syllablesPerWord - wordsPerSentence);
        return new ReadabilityScore(value, infleszLevel(value));
    }

    static String fernandezHuertaLevel(double value) {
        if (value >= 90) {
            return "muy fácil";
        }
        if (value >= 80) {
            return "fácil";
        }
        if (value >= 70) {
            return "bastante fácil";
        }
        if (value >= 60) {
            return "normal";
        }
        if (value >= 50) {
            return "bastante difícil";
        }
        if (value >= 30) {
            return "difícil";
        }
        return "muy difícil";
    }

    static String infleszLevel(double value) {
        if (value >= 80) {
            return "muy fácil";
        }
        if (value >= 65) {
            return "bastante fácil";
        }
        if (value >= 55) {
            return "normal";
        }
        if (value >= 40) {
            return "algo difícil";
        }
        return "muy difícil";
    }

    // Redondea a 1 decimal: 34.855 pasa a 34.9
    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
