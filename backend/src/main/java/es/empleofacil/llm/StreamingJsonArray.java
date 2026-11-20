package es.empleofacil.llm;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

// Lee una lista de textos de un JSON que llega a trozos, por ejemplo "professions": ["cocinero", "pinche"].
// accept() devuelve la lista una sola vez, en cuanto llega el corchete que la cierra.
// Así no hay que esperar al final del JSON para usarla.
public final class StreamingJsonArray {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Pattern start;
    private final StringBuilder raw = new StringBuilder();
    private boolean done;

    public StreamingJsonArray(String fieldName) {
        start = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\\[");
    }

    // Recibe un trozo del JSON. Devuelve la lista la primera vez que está completa; si no, vacío.
    public Optional<List<String>> accept(String chunk) {
        if (done) {
            return Optional.empty();
        }
        raw.append(chunk);
        Matcher matcher = start.matcher(raw);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int open = matcher.end() - 1;
        int close = closingBracket(raw, open);
        if (close < 0) {
            return Optional.empty();
        }
        done = true;
        try {
            return Optional.of(JSON.readValue(raw.substring(open, close + 1), new TypeReference<List<String>>() { }));
        } catch (RuntimeException e) {
            // La lista no es una lista de textos: hacemos como si no hubiera llegado
            return Optional.empty();
        }
    }

    // Busca el ] que cierra la lista. Un ] dentro de comillas no cuenta, y una comilla con barra delante tampoco cierra.
    private static int closingBracket(CharSequence text, int open) {
        boolean inString = false;
        for (int index = open + 1; index < text.length(); index++) {
            char character = text.charAt(index);
            if (inString) {
                if (character == '\\') {
                    index++;
                } else if (character == '"') {
                    inString = false;
                }
            } else if (character == '"') {
                inString = true;
            } else if (character == ']') {
                return index;
            }
        }
        return -1;
    }
}
