package es.empleofacil.lecturafacil.analysis;

import java.util.Locale;

// Cuenta las sílabas de una palabra en español. Lo necesitan los índices de legibilidad.
public final class SyllableCounter {

    // Marca que la letra anterior no era una vocal
    private static final char NO_VOWEL = '\0';

    private SyllableCounter() {
    }

    // Cuenta los grupos de vocales y suma 1 por cada hiato dentro de un grupo.
    // Una palabra sin vocales, como "8:00", cuenta como 1 sílaba.
    public static int count(String word) {
        String lowerCaseWord = word.toLowerCase(Locale.ROOT);
        int syllables = 0;
        char previous = NO_VOWEL;
        for (int i = 0; i < lowerCaseWord.length(); i++) {
            if (!soundsAsVowel(lowerCaseWord, i)) {
                previous = NO_VOWEL;
                continue;
            }
            char letter = lowerCaseWord.charAt(i);
            if (previous == NO_VOWEL || isHiatus(previous, letter)) {
                syllables++;
            }
            previous = letter;
        }
        return Math.max(syllables, 1);
    }

    // La "y" suena como vocal si no le sigue otra vocal: "hoy" sí, "ayuda" no.
    // La "u" de "que", "qui", "gue" y "gui" no suena.
    private static boolean soundsAsVowel(String word, int index) {
        char letter = word.charAt(index);
        char next = index + 1 < word.length() ? word.charAt(index + 1) : NO_VOWEL;
        if (letter == 'y') {
            return !isVowel(next);
        }
        if (letter == 'u' && index > 0 && "qg".indexOf(word.charAt(index - 1)) != -1
                && "eiéí".indexOf(next) != -1) {
            return false;
        }
        return isVowel(letter);
    }

    // Hay hiato si se juntan dos vocales fuertes ("poeta")
    // o una fuerte y una débil con tilde ("día", "país").
    private static boolean isHiatus(char first, char second) {
        return isStrong(first) && (isStrong(second) || isAccentedWeak(second))
                || isAccentedWeak(first) && isStrong(second);
    }

    private static boolean isStrong(char letter) {
        return "aeoáéó".indexOf(letter) != -1;
    }

    private static boolean isAccentedWeak(char letter) {
        return "íú".indexOf(letter) != -1;
    }

    // Devuelve true si la letra es una vocal y false si no lo es.
    // La letra siempre llega en minúscula.
    // Cuentan como vocales: a e i o u, también con tilde (á é í ó ú) y la ü de "pingüino".
    static boolean isVowel(char letter) {
        return "aeiouáéíóúü".indexOf(letter) != -1;
    }
}
