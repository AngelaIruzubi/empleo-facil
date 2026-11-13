package es.empleofacil.llm;

import java.time.Duration;
import java.util.Locale;

import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;

// Convierte los tiempos que devuelve Ollama en una línea de log fácil de leer:
// total 43.2 s | carga 0.0 s | lectura 2213 tokens en 0.7 s | escritura 225 tokens en 42.4 s
// Carga: subir el modelo a memoria. Lectura: procesar el prompt. Escritura: generar la respuesta.
public final class AiTimings {

    private AiTimings() {
    }

    public static String describe(ChatResponse response) {
        if (response == null || response.getMetadata() == null) {
            return "sin datos de tiempo";
        }
        ChatResponseMetadata metadata = response.getMetadata();
        return "total %s | carga %s | lectura %s tokens en %s | escritura %s tokens en %s".formatted(
                seconds(metadata.get("total-duration")),
                seconds(metadata.get("load-duration")),
                count(metadata.get("prompt-eval-count")),
                seconds(metadata.get("prompt-eval-duration")),
                count(metadata.get("eval-count")),
                seconds(metadata.get("eval-duration")));
    }

    // Ollama mide en nanosegundos. Spring AI lo puede guardar como Duration o como número.
    static String seconds(Object value) {
        if (value instanceof Duration duration) {
            return String.format(Locale.ROOT, "%.1f s", duration.toMillis() / 1000.0);
        }
        if (value instanceof Number nanoseconds) {
            return String.format(Locale.ROOT, "%.1f s", nanoseconds.longValue() / 1_000_000_000.0);
        }
        return "?";
    }

    private static String count(Object value) {
        return value == null ? "?" : value.toString();
    }
}
