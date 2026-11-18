package es.empleofacil.lecturafacil;

// La IA no ha podido corregir las frases marcadas. Es distinto de EasyReadException
// para que la persona lea "no podemos corregir" y no "no podemos adaptar".
public class RevisionException extends RuntimeException {

    public RevisionException(String message) {
        super(message);
    }

    public RevisionException(String message, Throwable cause) {
        super(message, cause);
    }
}
