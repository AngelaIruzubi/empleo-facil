package es.empleofacil.lecturafacil;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

// El preparador laboral ha marcado frases del texto adaptado que están mal.
// La IA corrige solo esas frases; el resto del texto no cambia.
// Los mensajes de error están en lectura fácil, como en EasyReadRequest.
public record ReviseRequest(
        @NotBlank(message = "Falta la oferta original.")
        @Size(max = EasyReadRequest.MAX_LENGTH, message = "La oferta original es muy larga.")
        String originalText,

        @NotBlank(message = "Falta el texto adaptado.")
        @Size(max = 2 * EasyReadRequest.MAX_LENGTH, message = "El texto adaptado es muy largo.")
        String adaptedText,

        @NotEmpty(message = "Marca al menos una frase para corregir.")
        @Size(max = 30, message = "Puedes corregir 30 frases como máximo.")
        List<@Valid Correction> corrections) {

    public record Correction(
            @NotBlank(message = "Falta la frase que hay que corregir.")
            @Size(max = 500, message = "La frase es muy larga.")
            String sentence,

            @NotBlank(message = "Escribe qué está mal en cada frase marcada.")
            @Size(max = 500, message = "El comentario es muy largo.\nEscribe 500 letras como máximo.")
            String comment) {
    }
}
