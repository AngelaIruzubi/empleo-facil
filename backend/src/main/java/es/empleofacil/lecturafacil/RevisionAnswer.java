package es.empleofacil.lecturafacil;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

// Lo que contesta la IA al corregir. Cada frase corregida lleva el número de la frase marcada.
// Si la IA divide una corrección en varias frases, todas llevan el mismo número y luego se juntan.
public record RevisionAnswer(
        @JsonPropertyDescription("Las frases corregidas")
        List<Item> sentences) {

    public record Item(
            @JsonPropertyDescription("El número de la frase marcada que corriges: 1, 2, 3...")
            int number,

            @JsonPropertyDescription("La frase corregida")
            String text) {
    }
}
