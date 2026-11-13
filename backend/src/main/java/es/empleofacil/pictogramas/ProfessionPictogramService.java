package es.empleofacil.pictogramas;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import es.empleofacil.llm.AiTimings;

// Busca el pictograma de la profesión de una oferta de trabajo.
// Es una llamada a la IA aparte de la adaptación: con una sola tarea, un modelo pequeño acierta más.
@Service
public class ProfessionPictogramService {

    private static final Logger log = LoggerFactory.getLogger(ProfessionPictogramService.class);

    static final int MAX_PROFESSIONS = 3;

    // Palabras que solas no dicen qué trabajo se hace. En ARASAAC, "auxiliar" es una monitora
    // de educación especial: ese pictograma en una oferta de limpieza confundiría a la persona.
    // El prompt ya las prohíbe, pero un modelo pequeño no siempre hace caso.
    static final Set<String> GENERIC_WORDS = Set.of(
            "auxiliar", "ayudante", "operario", "operaria", "empleado", "empleada",
            "personal", "trabajador", "trabajadora", "mozo", "moza", "peón");

    private final ChatClient chatClient;
    private final Resource systemPrompt;
    private final ArasaacClient arasaacClient;

    public ProfessionPictogramService(ChatClient chatClient,
                                      @Value("classpath:prompts/profession-system.st") Resource systemPrompt,
                                      ArasaacClient arasaacClient) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
        this.arasaacClient = arasaacClient;
    }

    public ProfessionPictogram find(String text) {
        List<String> professions = professions(text);
        if (professions.isEmpty()) {
            return ProfessionPictogram.NONE;
        }
        return new ProfessionPictogram(professions, arasaacClient.findFirstProfession(professions).orElse(null));
    }

    // Si la IA falla, seguimos sin pictograma: el pictograma ayuda, pero el texto adaptado ya está hecho
    List<String> professions(String text) {
        try {
            ResponseEntity<ChatResponse, Professions> response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(text)
                    // JSON nativo: Ollama obliga al modelo a escribir un JSON válido con el molde del record.
                    // Sin esto, el molde va como texto en el mensaje y a veces el JSON sale roto.
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .call()
                    .responseEntity(Professions.class);
            log.info("Tiempos de la IA en la profesión: {}", AiTimings.describe(response.getResponse()));
            Professions result = response.getEntity();
            if (result == null || result.professions() == null) {
                return List.of();
            }
            return result.professions().stream()
                    .filter(profession -> profession != null && !profession.isBlank())
                    .map(profession -> profession.strip().toLowerCase(Locale.ROOT))
                    .filter(profession -> !GENERIC_WORDS.contains(profession))
                    .distinct()
                    .limit(MAX_PROFESSIONS)
                    .toList();
        } catch (RuntimeException e) {
            log.warn("La IA no ha podido decir la profesión del texto", e);
            return List.of();
        }
    }

    record Professions(
            @JsonPropertyDescription("De 1 a 3 nombres de la profesión, en singular. Primero el más general y corto. "
                    + "Lista vacía si el texto no es una oferta de trabajo.")
            List<String> professions) {
    }
}
