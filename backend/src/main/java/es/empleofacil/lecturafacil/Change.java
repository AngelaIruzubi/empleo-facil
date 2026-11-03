package es.empleofacil.lecturafacil;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

// Un cambio que hizo el motor al adaptar el texto. Sirve para que una persona lo revise.
// Las descripciones van en el JSON Schema que Spring AI envía al modelo.
public record Change(
        @JsonPropertyDescription("Trozo del texto original")
        String original,

        @JsonPropertyDescription("Cómo queda ese trozo en el texto adaptado")
        String adapted,

        @JsonPropertyDescription("Por qué se ha cambiado. Empieza por 'Revisar:' si no estás seguro del significado")
        String reason) {
}
