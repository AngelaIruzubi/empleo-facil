package es.empleofacil.lecturafacil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Los mensajes de error están en lectura fácil porque los puede leer la persona usuaria
public record EasyReadRequest(
        @NotBlank(message = "Escribe un texto para adaptar.")
        @Size(max = EasyReadRequest.MAX_LENGTH,
              message = "El texto es muy largo.\nPuedes adaptar 4000 letras como máximo.\nDivide el texto en partes más pequeñas.")
        String text) {

    static final int MAX_LENGTH = 4000;
}
