package es.empleofacil.lecturafacil;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

// La respuesta de la IA al corregir: una frase corregida por cada frase marcada, en el mismo orden
public record RevisedSentences(
        @JsonPropertyDescription("Las frases corregidas, una por cada frase que hay que corregir y en el mismo orden")
        List<String> sentences) {
}
