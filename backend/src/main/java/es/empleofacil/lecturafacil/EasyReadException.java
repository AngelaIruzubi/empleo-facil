package es.empleofacil.lecturafacil;

// Error al adaptar un texto con la IA: Ollama apagado, demasiado tiempo o respuesta que no se entiende
public class EasyReadException extends RuntimeException {

    public EasyReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public EasyReadException(String message) {
        super(message);
    }
}
