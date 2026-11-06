package es.empleofacil.lecturafacil.analysis;

// Lo que medimos de un texto y sus dos notas de legibilidad.
public record Readability(
        int sentences,
        int words,
        int syllables,
        ReadabilityScore fernandezHuerta,
        ReadabilityScore inflesz) {
}
