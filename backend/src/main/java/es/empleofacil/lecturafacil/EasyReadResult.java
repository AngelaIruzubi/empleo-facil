package es.empleofacil.lecturafacil;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record EasyReadResult(
        @JsonPropertyDescription("El texto adaptado a lectura fácil, con cada frase en una línea")
        String adaptedText,

        @JsonPropertyDescription("Los cambios importantes que has hecho al adaptar el texto")
        List<Change> changes) {
}
