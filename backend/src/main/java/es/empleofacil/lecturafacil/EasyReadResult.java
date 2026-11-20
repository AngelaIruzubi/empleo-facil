package es.empleofacil.lecturafacil;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// La profesión va la primera: Ollama escribe los campos en el orden del molde (JSON Schema).
// Así llega en los primeros segundos y el pictograma sale antes que el texto.
// Antes la profesión se pedía en una llamada aparte, que volvía a leer toda la oferta (unos 25 s más).
@JsonPropertyOrder({ "professions", "adaptedText", "changes" })
public record EasyReadResult(
        @JsonPropertyDescription("Si el texto es una oferta de trabajo, de 1 a 3 nombres de la profesión, "
                + "en singular y en minúsculas. Primero el más corto. Lista vacía si no es una oferta de trabajo.")
        List<String> professions,

        @JsonPropertyDescription("El texto adaptado a lectura fácil, con cada frase en una línea")
        String adaptedText,

        @JsonPropertyDescription("Los cambios importantes que has hecho al adaptar el texto")
        List<Change> changes) {

    // Si la IA no escribe el campo, la lista queda vacía en vez de null
    public EasyReadResult {
        professions = professions == null ? List.of() : professions;
    }

    // Sin profesiones: para los tests y para quien no las necesita
    public EasyReadResult(String adaptedText, List<Change> changes) {
        this(List.of(), adaptedText, changes);
    }
}
