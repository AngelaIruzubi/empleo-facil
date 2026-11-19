package es.empleofacil.lecturafacil;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

// Lo que contesta la IA al corregir UNA frase marcada.
// Antes pedíamos todas las frases a la vez, numeradas, y el modelo pequeño se liaba con los números.
public record RevisionAnswer(
        @JsonPropertyDescription("La frase corregida en lectura fácil. Si necesitas varias frases, escribe cada una en una línea")
        String correctedText) {
}
