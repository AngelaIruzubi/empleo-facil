package es.empleofacil.llm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Lee el valor de un campo de texto de un JSON que llega a trozos.
// Ejemplo: llegan '{"adaptedText": "Hola\' y luego 'nmundo", ...'.
// accept() devuelve "Hola" la primera vez y "\nmundo" la segunda.
// Si un trozo corta un escape por la mitad (la barra sola, o un código de letra a medias), espera al siguiente.
public final class StreamingJsonField {

    private final Pattern start;
    private final StringBuilder raw = new StringBuilder();
    // Dónde seguir leyendo dentro de raw. -1: el campo todavía no ha empezado.
    private int position = -1;
    private boolean finished;

    public StreamingJsonField(String fieldName) {
        start = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"");
    }

    // Recibe un trozo del JSON y devuelve el texto nuevo del campo, ya sin escapes
    public String accept(String chunk) {
        raw.append(chunk);
        if (finished) {
            return "";
        }
        if (position < 0) {
            Matcher matcher = start.matcher(raw);
            if (!matcher.find()) {
                return "";
            }
            position = matcher.end();
        }
        StringBuilder text = new StringBuilder();
        while (position < raw.length()) {
            char current = raw.charAt(position);
            if (current == '"') {
                finished = true;
                break;
            }
            if (current != '\\') {
                text.append(current);
                position++;
                continue;
            }
            if (position + 1 >= raw.length()) {
                break;
            }
            char escaped = raw.charAt(position + 1);
            if (escaped == 'u') {
                if (position + 6 > raw.length()) {
                    break;
                }
                text.append((char) Integer.parseInt(raw.substring(position + 2, position + 6), 16));
                position += 6;
            } else {
                text.append(switch (escaped) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    default -> escaped; // \" \\ \/
                });
                position += 2;
            }
        }
        return text.toString();
    }
}
