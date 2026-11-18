package es.empleofacil.lecturafacil;

// La persona ha cerrado o recargado la página mientras la IA trabajaba.
// No es un error: paramos la IA para que no siga trabajando para nadie.
public class ClientDisconnectedException extends RuntimeException {

    public ClientDisconnectedException(Throwable cause) {
        super("La página se ha cerrado", cause);
    }
}
