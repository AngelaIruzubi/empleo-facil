package es.empleofacil.lecturafacil.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Parte un texto en frases y en palabras. Es la base de los índices de legibilidad y de las reglas.
public final class TextSplitter {

    // Fin de frase: . ! ? o … seguidos de un espacio, un salto de línea o el final del texto.
    // Así "7.5" o "www.sepe.es" no cortan la frase.
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?…]+[\"»”)]*(?=\\s|$)");

    // Palabra: letras o números. Puede llevar dentro . , : ' o - entre letras o números,
    // para que "7,5", "8:00" o "socio-sanitario" cuenten como una sola palabra.
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+(?:[.,:'’-][\\p{L}\\p{N}]+)*");

    private static final Pattern HAS_LETTER_OR_NUMBER = Pattern.compile("[\\p{L}\\p{N}]");

    // Abreviaturas cuyo punto no acaba la frase. Se comparan en minúsculas.
    private static final Set<String> ABBREVIATIONS = Set.of(
            "sr", "sra", "srta", "dr", "dra", "d", "dña", "ud", "uds",
            "p", "ej", "núm", "nº", "pág", "tel", "tfno", "aprox", "avda", "c");

    private TextSplitter() {
    }

    // Cada línea es al menos una frase: en lectura fácil cada frase va en su línea
    // y los elementos de una lista no suelen llevar punto.
    public static List<String> sentences(String text) {
        List<String> sentences = new ArrayList<>();
        for (String line : text.split("\\R")) {
            int start = 0;
            Matcher end = SENTENCE_END.matcher(line);
            while (end.find()) {
                if (end.group().equals(".") && endsWithAbbreviation(line, end.start())) {
                    continue;
                }
                addIfNotEmpty(sentences, line.substring(start, end.end()));
                start = end.end();
            }
            addIfNotEmpty(sentences, line.substring(start));
        }
        return sentences;
    }

    public static List<String> words(String text) {
        List<String> words = new ArrayList<>();
        Matcher word = WORD.matcher(text);
        while (word.find()) {
            words.add(word.group());
        }
        return words;
    }

    // Mira la palabra que hay justo antes del punto: "Sra." o una inicial como "J."
    private static boolean endsWithAbbreviation(String line, int dotIndex) {
        int wordStart = dotIndex;
        while (wordStart > 0 && Character.isLetter(line.charAt(wordStart - 1))) {
            wordStart--;
        }
        String word = line.substring(wordStart, dotIndex);
        if (word.isEmpty()) {
            return false;
        }
        boolean isInitial = word.length() == 1 && Character.isUpperCase(word.charAt(0));
        return isInitial || ABBREVIATIONS.contains(word.toLowerCase(Locale.ROOT));
    }

    private static void addIfNotEmpty(List<String> sentences, String sentence) {
        String trimmed = sentence.strip();
        if (HAS_LETTER_OR_NUMBER.matcher(trimmed).find()) {
            sentences.add(trimmed);
        }
    }
}
