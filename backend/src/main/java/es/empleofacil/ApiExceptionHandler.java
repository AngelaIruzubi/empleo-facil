package es.empleofacil;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import es.empleofacil.lecturafacil.EasyReadException;

// Convierte los errores de todos los controladores en respuestas claras y en lectura fácil.
// Usa ProblemDetail, el formato estándar para errores en APIs web (RFC 9457).
@RestControllerAdvice
public class ApiExceptionHandler {

    // 400: los datos enviados no cumplen las reglas, por ejemplo un texto vacío
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidRequest(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Revisa los datos que has enviado.");
        return problem(HttpStatus.BAD_REQUEST, "Revisa el texto", detail);
    }

    // 400: la petición no es un JSON que se pueda leer
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "Revisa el texto",
                "No entendemos lo que has enviado.\nRevisa el texto y envíalo otra vez.");
    }

    // 503: la IA ha fallado. Se puede volver a intentar más tarde.
    @ExceptionHandler(EasyReadException.class)
    public ProblemDetail handleEasyReadError(EasyReadException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "No se puede adaptar el texto ahora",
                "Ahora no podemos adaptar el texto.\nInténtalo otra vez dentro de un rato.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
