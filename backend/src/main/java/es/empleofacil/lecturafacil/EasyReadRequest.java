package es.empleofacil.lecturafacil;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Los mensajes de error están en lectura fácil porque los puede leer la persona usuaria
public record EasyReadRequest(
        // El texto tiene que tener al menos una letra o un número: "..." no se puede adaptar ni medir
        @NotNull(message = EasyReadRequest.EMPTY_TEXT)
        @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*", message = EasyReadRequest.EMPTY_TEXT)
        @Size(max = EasyReadRequest.MAX_LENGTH,
              message = "El texto es muy largo.\nPuedes adaptar 4000 letras como máximo.\nDivide el texto en partes más pequeñas.")
        String text) {

    static final int MAX_LENGTH = 4000;
    static final String EMPTY_TEXT = "Escribe un texto para adaptar.";
}
